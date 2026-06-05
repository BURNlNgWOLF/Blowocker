package com.example.overlay.domain.usecase

import com.example.overlay.domain.repository.SystemRepository

class NavigateHomeUseCase(private val repository: SystemRepository) {
    operator fun invoke() {
        if (repository.isAccessibilityServiceAvailable()) {
            val success = repository.navigateToHomeViaAccessibility()
            if (!success) {
                repository.navigateToHomeViaIntent()
            }
        } else {
            repository.navigateToHomeViaIntent()
        }
    }
}