//
//  SpeedBubble.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI

struct IndicatorBubble: View {
    let value: Int
    let unit: String
    let size: CGFloat
    
    var body: some View {
        VStack {
            Text("\(value)")
                .font(.system(size: size * 0.38, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.primary)
            
            Text(unit)
                .font(.system(size: size * 0.16, weight: .medium, design: .rounded))
                .foregroundStyle(.secondary)
        }
        .frame(width: size, height: size)
        .contentShape(Circle())
        .modifier(GlassBubbleStyle(shape: .circle))
    }
}
