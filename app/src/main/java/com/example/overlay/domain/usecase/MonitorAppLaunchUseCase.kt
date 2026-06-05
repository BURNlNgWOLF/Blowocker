package com.example.overlay.domain.usecase

import com.example.overlay.domain.repository.SystemRepository
import kotlinx.coroutines.flow.collectLatest

class MonitorAppLaunchUseCase(private val repository: SystemRepository) {
    suspend operator fun invoke(targetPackages: List<String>, onMatch: () -> Unit) {
        repository.observeCurrentPackage().collectLatest { packageName ->
            if (targetPackages.contains(packageName)) {
                onMatch()
            }
        }
    }
}