#!/usr/bin/env python3
"""Record and replay MSFS UDP location streams for PigeonNavigator.

The recording format is JSON Lines. Each line contains one UDP datagram with
absolute and relative timestamps plus a base64-encoded payload, so arbitrary
UDP bytes are preserved exactly.
"""

from __future__ import annotations

import argparse
import base64
import json
import signal
import socket
import sys
import time
from pathlib import Path
from typing import Any


DEFAULT_BIND_HOST = "0.0.0.0"
DEFAULT_TARGET_HOST = "127.0.0.1"
DEFAULT_PORT = 49002
BUFFER_SIZE = 65_535
FORMAT_VERSION = 1


def positive_float(value: str) -> float:
    parsed = float(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be greater than 0")
    return parsed


def non_negative_float(value: str) -> float:
    parsed = float(value)
    if parsed < 0:
        raise argparse.ArgumentTypeError("must be greater than or equal to 0")
    return parsed


def positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be greater than 0")
    return parsed


def now_ns() -> int:
    return time.time_ns()


def monotonic_ns() -> int:
    return time.monotonic_ns()


def record(args: argparse.Namespace) -> int:
    stop = False

    def request_stop(signum: int, frame: Any) -> None:
        nonlocal stop
        stop = True

    signal.signal(signal.SIGINT, request_stop)
    signal.signal(signal.SIGTERM, request_stop)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((args.host, args.port))
        sock.settimeout(0.5)

        started_wall_ns = now_ns()
        started_mono_ns = monotonic_ns()
        received = 0

        with output_path.open("w", encoding="utf-8") as file:
            header = {
                "type": "header",
                "format": "pigeonnavigator-udp-jsonl",
                "version": FORMAT_VERSION,
                "created_time_ns": started_wall_ns,
                "bind_host": args.host,
                "bind_port": args.port,
                "source": "msfs",
            }
            file.write(json.dumps(header, separators=(",", ":")) + "\n")
            file.flush()

            print(f"Recording UDP on {args.host}:{args.port} -> {output_path}")
            print("Stop with Ctrl+C.")

            while not stop:
                if args.duration is not None:
                    elapsed_s = (monotonic_ns() - started_mono_ns) / 1_000_000_000
                    if elapsed_s >= args.duration:
                        break
                if args.max_packets is not None and received >= args.max_packets:
                    break

                try:
                    payload, address = sock.recvfrom(BUFFER_SIZE)
                except socket.timeout:
                    continue

                packet_mono_ns = monotonic_ns()
                packet = {
                    "type": "packet",
                    "time_ns": now_ns(),
                    "relative_ns": packet_mono_ns - started_mono_ns,
                    "source_host": address[0],
                    "source_port": address[1],
                    "size": len(payload),
                    "payload_b64": base64.b64encode(payload).decode("ascii"),
                }
                file.write(json.dumps(packet, separators=(",", ":")) + "\n")
                received += 1

                if args.flush_each:
                    file.flush()

        print(f"Recorded {received} packet(s).")
    return 0


def iter_packets(input_path: Path):
    with input_path.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            line = line.strip()
            if not line:
                continue

            try:
                item = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"Invalid JSON on line {line_number}: {exc}") from exc

            if item.get("type") != "packet":
                continue

            if "relative_ns" not in item or "payload_b64" not in item:
                raise ValueError(f"Incomplete packet on line {line_number}")

            payload = base64.b64decode(item["payload_b64"], validate=True)
            yield int(item["relative_ns"]), payload


def replay(args: argparse.Namespace) -> int:
    input_path = Path(args.input)
    target = (args.host, args.port)
    packets_sent = 0
    previous_relative_ns: int | None = None

    print(f"Replaying {input_path} -> {args.host}:{args.port} at {args.speed:g}x")

    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        for relative_ns, payload in iter_packets(input_path):
            if previous_relative_ns is None:
                delay_s = args.initial_delay
            else:
                delta_ns = max(0, relative_ns - previous_relative_ns)
                delay_s = (delta_ns / 1_000_000_000) / args.speed

            if delay_s > 0:
                time.sleep(delay_s)

            sock.sendto(payload, target)
            packets_sent += 1
            previous_relative_ns = relative_ns

            if args.max_packets is not None and packets_sent >= args.max_packets:
                break

    print(f"Replayed {packets_sent} packet(s).")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Record and replay MSFS UDP streams for PigeonNavigator.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    record_parser = subparsers.add_parser("record", help="record UDP datagrams")
    record_parser.add_argument("output", help="JSONL recording path")
    record_parser.add_argument("--host", default=DEFAULT_BIND_HOST, help=f"bind host, default {DEFAULT_BIND_HOST}")
    record_parser.add_argument("--port", type=positive_int, default=DEFAULT_PORT, help=f"bind port, default {DEFAULT_PORT}")
    record_parser.add_argument("--duration", type=non_negative_float, help="recording duration in seconds")
    record_parser.add_argument("--max-packets", type=positive_int, help="stop after this many packets")
    record_parser.add_argument("--flush-each", action="store_true", help="flush file after each packet")
    record_parser.set_defaults(func=record)

    replay_parser = subparsers.add_parser("replay", help="replay recorded UDP datagrams")
    replay_parser.add_argument("input", help="JSONL recording path")
    replay_parser.add_argument("--host", default=DEFAULT_TARGET_HOST, help=f"target host, default {DEFAULT_TARGET_HOST}")
    replay_parser.add_argument("--port", type=positive_int, default=DEFAULT_PORT, help=f"target port, default {DEFAULT_PORT}")
    replay_parser.add_argument("--speed", type=positive_float, default=1.0, help="playback speed multiplier, default 1")
    replay_parser.add_argument("--initial-delay", type=non_negative_float, default=0.0, help="delay before first packet in seconds")
    replay_parser.add_argument("--max-packets", type=positive_int, help="replay at most this many packets")
    replay_parser.set_defaults(func=replay)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return args.func(args)
    except (OSError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
