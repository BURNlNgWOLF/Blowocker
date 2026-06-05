package com.example.overlay.domain.usecase

import com.example.overlay.domain.repository.SystemRepository

class StartOverlayUseCase(private val repository: SystemRepository) {
    operator fun invoke(delayMs: Int = 5_000) {
        if (repository.hasOverlayPermission()) {
            repository.startOverlayService(delayMs)
        }
    }
}