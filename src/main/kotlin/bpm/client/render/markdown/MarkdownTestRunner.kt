package bpm.client.render.markdown

import bpm.client.docs.Docs
import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime.logger
import bpm.common.utils.FontAwesome
import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiConfigFlags
import java.io.File

fun main(args: Array<String>) {
    Application.launch(object : Application() {

        private lateinit var markdownBrowser: MarkdownBrowser

        private fun initializeFonts() {
            try {
                Fonts.register(
                    "Inter",
                    8..50,
                    "Regular",
                    "Bold",
                    "Italic",
                    "BoldItalic",
                    "SemiBold",
                    "ExtraBold",
                    "Medium"
                )
                Fonts.register("Minecraft", 8..50, "Title", "Body")
                Fonts.register(
                    "Fa",
                    24..84,
                    FontAwesome.IconMin to FontAwesome.IconMax,
                    true,
                    "Regular"
                )
                Fonts.registerFonts()
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize fonts" }
                throw RuntimeException("Font initialization failed", e)
            }
        }

        override fun configure(config: Configuration) {
            config.setTitle("Markdown Browser")
//            config.setWindowIcon("icon.png")  // Make sure to have an icon file in your resources
        }

        override fun initImGui(config: Configuration?) {
            super.initImGui(config)
            initializeFonts()
            Fonts.registerFonts()

            val io = ImGui.getIO()
            io.configFlags = io.configFlags or ImGuiConfigFlags.NavEnableKeyboard
        }

        override fun init(config: Configuration?) {
            super.init(config)
            val docs = Docs(File("D:\\meng.core\\src\\main\\resources\\docs").toPath())
            docs.onInstall()
            markdownBrowser = MarkdownBrowser(
                docs
            )
        }

        override fun process() {
            markdownBrowser.render()
        }

        override fun dispose() {
            super.dispose()
        }
    })
}