//
//  HUDCluster.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI

struct HUDCluster: View {
    let speed: Int
    let altitude: Int
    
    private let bubbleSize: CGFloat = 64
    private let gap: Int = 8
    
    var body: some View {
        let gapCGFloat = CGFloat(gap)
        let yOffset = -(bubbleSize / 2 + bubbleSize / 2 + gapCGFloat)
        
        ZStack(alignment: .bottomLeading) {
            IndicatorBubble(value: 0, unit: "km/h", size: bubbleSize)
            IndicatorBubble(value: 0, unit: "m", size: bubbleSize)
                .offset(x: 0, y: yOffset)
        }
    }
}

