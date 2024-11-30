package bpm.client.render.panels

interface IModal {

    var isModal: Boolean
    var modalBackgroundColor: Int

    fun showModal()
    fun hideModal()
    fun onModalBackgroundClick()
}