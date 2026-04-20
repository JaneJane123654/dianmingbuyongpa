package com.classroomassistant.composeapp.navigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.classroomassistant.composeapp.presenters.LauncherPresenter
import com.classroomassistant.composeapp.presenters.PlaceholderPresenter
import com.classroomassistant.composeapp.presenters.RootDestination

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun navigateTo(destination: RootDestination)

    fun onBackRequested()

    sealed interface Child {
        data class Launcher(val presenter: LauncherPresenter) : Child

        data class Monitoring(val presenter: PlaceholderPresenter) : Child

        data class Settings(val presenter: PlaceholderPresenter) : Child

        data class Models(val presenter: PlaceholderPresenter) : Child

        data class Diagnostics(val presenter: PlaceholderPresenter) : Child
    }
}
