//
//  HUDCluster.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI
import CoreLocation

struct HUDCluster: View {
    let speed: Int
    let speedUnit: String
    let altitude: Int
    let altitudeUnit: String
    let mapDirection: CLLocationDirection
    let isRecenterVisible: Bool
    let onCompassTap: () -> Void
    let onRecenterTap: () -> Void
    
    private let bubbleSize: CGFloat = 64
    private let gap: CGFloat = 8

    private var normalizedDirection: CLLocationDirection {
        let value = mapDirection.truncatingRemainder(dividingBy: 360)
        return value >= 0 ? value : value + 360
    }

    private var isCompassVisible: Bool {
        let distanceToNorth = min(normalizedDirection, 360 - normalizedDirection)
        return distanceToNorth > 0.05
    }
    
    var body: some View {
        HStack(alignment: .bottom) {
            VStack(spacing: gap) {
                IndicatorBubble(value: altitude, unit: altitudeUnit, size: bubbleSize)
                IndicatorBubble(value: speed, unit: speedUnit, size: bubbleSize)
            }

            Spacer(minLength: 0)

            VStack(spacing: gap) {
                if isRecenterVisible {
                    RecenterBubble(size: bubbleSize, action: onRecenterTap)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }

                if isCompassVisible {
                    CompassBubble(direction: normalizedDirection, size: bubbleSize, action: onCompassTap)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
        }
        .animation(.spring(response: 0.28, dampingFraction: 0.9), value: isCompassVisible)
        .animation(.spring(response: 0.28, dampingFraction: 0.9), value: isRecenterVisible)
    }
}
