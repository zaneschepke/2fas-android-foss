package com.twofasapp.widgets.configure

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mikepenz.fastadapter.binding.ModelAbstractBindingItem
import com.twofasapp.resources.R
import com.twofasapp.databinding.ItemWidgetSettingsServiceBinding

class WidgetSettingsServiceItem(model: WidgetSettingsService) :
    ModelAbstractBindingItem<WidgetSettingsService, ItemWidgetSettingsServiceBinding>(model) {

    override val type = R.id.item_widget_settings_service
    override var identifier = model.id

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?) =
        ItemWidgetSettingsServiceBinding.inflate(inflater, parent, false)

    override fun bindView(binding: ItemWidgetSettingsServiceBinding, payloads: List<Any>) {
        with(binding) {
            name.text = model.name
            switchButton.isChecked = model.isChecked
            root.setOnClickListener { model.switchAction?.invoke(model, switchButton.isChecked.not()) }
        }
    }
}