import SwiftUI

@ViewBuilder
func detailHeader(title: String, onBack: @escaping () -> Void) -> some View {
    HStack(spacing: 8) {
        Button(action: onBack) {
            Image(systemName: "chevron.backward")
                .font(.system(size: 14, weight: .semibold))
        }
        .buttonStyle(.plain)

        Text(title)
            .font(.system(size: 16, weight: .semibold))

        Spacer()
    }
    .padding(.horizontal, 4)
    .padding(.top, 8)
}

@ViewBuilder
func detailRow(label: String, value: String) -> some View {
    HStack(alignment: .firstTextBaseline, spacing: 12) {
        Text(label)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.secondary)
            .frame(minWidth: 86, alignment: .leading)
        Text(value)
            .font(.system(size: 13))
    }
}

@ViewBuilder
func detailActionButton(
    title: String,
    systemImage: String,
    action: @escaping () -> Void
) -> some View {
    Button(action: action) {
        Label(title, systemImage: systemImage)
            .font(.system(size: 14, weight: .semibold))
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
    .foregroundStyle(.white)
    .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
}
