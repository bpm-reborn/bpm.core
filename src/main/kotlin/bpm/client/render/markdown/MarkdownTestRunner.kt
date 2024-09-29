//package bpm.client.render.markdown
//
//import bpm.client.font.Fonts
//import bpm.client.runtime.ClientRuntime.logger
//import bpm.common.utils.FontAwesome
//import imgui.ImGui
//import imgui.app.Application
//import imgui.app.Configuration
//import imgui.flag.ImGuiConfigFlags
//
//fun main(args: Array<String>) {
//    Application.launch(object : Application() {
//
//        private lateinit var markdownBrowser: MarkdownBrowser
//
//        private fun initializeFonts() {
//            try {
//                Fonts.register("Inter", 8..50, "Regular", "Bold", "Italic", "BoldItalic")
//                Fonts.register("Minecraft", 8..50, "Title", "Body")
//                Fonts.register(
//                    "Fa",
//                    24..84,
//                    FontAwesome.IconMin to FontAwesome.IconMax,
//                    true,
//                    "Regular"
//                )
//                Fonts.registerFonts()
//            } catch (e: Exception) {
//                logger.error(e) { "Failed to initialize fonts" }
//                throw RuntimeException("Font initialization failed", e)
//            }
//        }
//
//        override fun configure(config: Configuration) {
//            config.setTitle("Markdown Browser")
////            config.setWindowIcon("icon.png")  // Make sure to have an icon file in your resources
//        }
//
//        override fun initImGui(config: Configuration?) {
//            super.initImGui(config)
//            initializeFonts()
//            Fonts.registerFonts()
//
//            val io = ImGui.getIO()
//            io.configFlags = io.configFlags or ImGuiConfigFlags.NavEnableKeyboard
//        }
//
//        override fun init(config: Configuration?) {
//            super.init(config)
//            markdownBrowser = MarkdownBrowser()
//
//            // Add sample Markdown files
//            markdownBrowser.addMarkdownFile(
//                "overview.md", """
//                ![BPM Reborn Logo](https://i.imgur.com/Ad1iQ6x.png)
//
//                # Overview
//                BPM Reborn is a core mod for Minecraft, designed to provide modders with a powerful framework for developing complex gameplay mechanics.
//
//                ## Node Graph System
//                BPM Reborn introduces an advanced node graph system, similar to the Blueprint nodes in Unreal Engine. This system allows modders to visually design and implement gameplay logic, making it easier to create advanced interactions between entities and systems without requiring deep programming knowledge.
//
//                ## Visual Gameplay Development
//                With BPM Reborn's node graph system, the complexity of gameplay mechanics becomes more accessible through visual tools, making modding both intuitive and powerful. Modders can achieve impressive results without writing extensive code.
//
//                ## Extensible Architecture
//                BPM Reborn is designed to be highly extensible, allowing modders to create custom nodes, actions, and conditions to suit their specific needs. The mod provides a robust API for creating custom gameplay mechanics and systems.
//            """.trimIndent()
//            )
//
//            markdownBrowser.addMarkdownFile(
//                "core/features.md", """
//                ![BPM Reborn Logo](https://i.imgur.com/Ad1iQ6x.png)
//
//                # Features
//
//                ## Advanced Node System
//                - Visual programming interface
//                - Drag-and-drop node creation
//                - Real-time node execution preview
//
//                ## Extensive Node Library
//                - Math operations
//                - Logic gates
//                - Entity manipulation
//                - World interaction nodes
//
//                ## Custom Node Creation
//                - API for defining new node types
//                - Integration with Java/Kotlin code
//
//                ## Performance Optimization
//                - Efficient node execution engine
//                - Multithreading support for parallel node processing
//
//                ## Mod Integration
//                - Compatibility layers for popular Minecraft mods
//                - Event system for inter-mod communication
//
//                ## Documentation and Tutorials
//                - Comprehensive wiki
//                - Video tutorials for beginners
//                - Advanced guides for experienced modders
//            """.trimIndent()
//            )
//
//            markdownBrowser.addMarkdownFile(
//                "core/quickstart.md", """
//                ![BPM Reborn Logo](https://i.imgur.com/Ad1iQ6x.png)
//
//                # Quick Start
//
//                ## Installation
//                1. Download the BPM Reborn mod from the official website.
//                2. Install the mod in your Minecraft instance.
//                3. Launch the game and create a new world.
//
//                ## Creating a Node Graph
//                1. Open the BPM Reborn node editor.
//                2. Create a new node graph project.
//                3. Add nodes to the graph and connect them to define gameplay logic.
//
//                ## Running the Node Graph
//                1. Press the play button to execute the node graph.
//                2. Observe the real-time preview of the node execution.
//                3. Debug and refine your gameplay logic as needed.
//
//                ## Saving and Exporting
//                1. Save your node graph project for future editing.
//                2. Export the node graph to a mod file for use in Minecraft.
//                3. Share your creations with the BPM Reborn community!
//            """.trimIndent()
//            )
//
//            markdownBrowser.addMarkdownFile(
//                "ghostbusters.md", """
//                ![center](https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNnlocWl3ZjU2OTExbnRuM2t2cm9pbGR3ZnhnZDF6NzB6NjlhMGs3YyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/7Hwp5jP0FtgAw/giphy.gif)
//
//                # Ghostbusters
//                If there's something strange in your neighborhood
//                Who you gonna call?
//                If there's something weird and it don't look good
//                Who you gonna call?
//                I ain't afraid of no ghost
//                I ain't afraid of no ghost
//                If you're seeing things running through your head
//                Who can you call?
//                ---
//
//                ![left](https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExMzFxcDhmbW42Zjh6Mmd2eGxzazh2ejJjMHltcXo2NGE4M2tpZmxzbiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/MxYQrB9jeGzza/giphy.gif)
//
//                ---
//                ```
//                local call = "Ghostbusters!"
//                print(call)
//                ```
//                ---
//
//                ![right](https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExYm9ubXNpbTl3dmk4djViY3Z3N3N6aHI3amJueGU1Zng2MWx2bXl5NCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/IF2PTnnBOUK5i/giphy.gif)
//            """.trimIndent()
//            )
//        }
//
//        override fun process() {
//            markdownBrowser.render()
//        }
//
//        override fun dispose() {
//            super.dispose()
//        }
//    })
//}