//
//  GlassBubbleStyle.swift
//  iosApp
//
//  Created by Miroslav Pašek on 07.02.2026.
//

import SwiftUI

struct GlassBubbleStyle<S: Shape>: ViewModifier {
    let shape: S
    
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: shape)
        } else {
            content
                .background(.ultraThinMaterial, in: shape)
                .overlay(shape.stroke(.white.opacity(0.25), lineWidth: 1))
        }
    }
}
