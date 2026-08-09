package ch.coredump.watertemp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

/**
 * A screen showing a single informational message.
 */
class MessageScreen(
    carContext: CarContext,
    private val title: String,
    private val message: String,
) : Screen(carContext) {

    override fun onGetTemplate(): Template =
        MessageTemplate.Builder(message)
            .setTitle(title)
            .setHeaderAction(Action.BACK)
            .build()
}
