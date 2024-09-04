package bpm.client.theme

import imgui.ImGui
import imgui.flag.ImGuiCol

import org.joml.Vector2f
import org.joml.Vector4f
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


fun themed(block: Theme.() -> Unit): Theme {
    val theme = Theme()
    theme.apply(block)
    return theme
}



class Theme() {

    val themeColors: EnumMap<ThemeColor, Vector4f> = EnumMap(ThemeColor::class.java)
    val themeStyle: EnumMap<ThemeStyle, Any> = EnumMap(ThemeStyle::class.java)

    inline fun themed(crossinline block: () -> Unit) {
        var totalColors = 0
        themeColors.forEach { (theme, color) ->
            ImGui.pushStyleColor(theme.colorId, color.x, color.y, color.z, color.w)
            totalColors++
        }
        var totalStyles = 0
        themeStyle.forEach { (theme, style) ->
            when (style) {
                is Float -> ImGui.pushStyleVar(theme.varId, style)
                is Vector2f -> ImGui.pushStyleVar(theme.varId, style.x, style.y)
            }
            totalStyles++
        }
//        if (fontName != null)
//            ImGui.pushFont(Fonts.fonts[fontName])


        block()
        ImGui.popStyleColor(totalColors)
        ImGui.popStyleVar(totalStyles)
//        if (fontName != null)
//            ImGui.popFont()
    }


    fun alpha(alpha: Float): Theme {
        themeStyle[ThemeStyle.Alpha] = alpha
        return this
    }

    fun disabledAlpha(disabledAlpha: Float): Theme {
        themeStyle[ThemeStyle.DisabledAlpha] = disabledAlpha
        return this
    }

    fun windowPadding(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.WindowPadding] = Vector2f(x, y)
        return this
    }

    fun windowRounding(windowRounding: Float): Theme {
        themeStyle[ThemeStyle.WindowRounding] = windowRounding
        return this
    }

    fun windowBorderSize(windowBorderSize: Float): Theme {
        themeStyle[ThemeStyle.WindowBorderSize] = windowBorderSize
        return this
    }

    fun windowMinSize(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.WindowMinSize] = Vector2f(x, y)
        return this
    }


    fun windowTitleAlign(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.WindowTitleAlign] = Vector2f(x, y)
        return this
    }


    fun childRounding(childRounding: Float): Theme {
        themeStyle[ThemeStyle.ChildRounding] = childRounding
        return this
    }

    fun childBorderSize(childBorderSize: Float): Theme {
        themeStyle[ThemeStyle.ChildBorderSize] = childBorderSize
        return this
    }

    fun popupRounding(popupRounding: Float): Theme {
        themeStyle[ThemeStyle.PopupRounding] = popupRounding
        return this
    }

    fun popupBorderSize(popupBorderSize: Float): Theme {
        themeStyle[ThemeStyle.PopupBorderSize] = popupBorderSize
        return this
    }

    fun framePadding(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.FramePadding] = Vector2f(x, y)
        return this
    }

    fun frameRounding(frameRounding: Float): Theme {
        themeStyle[ThemeStyle.FrameRounding] = frameRounding
        return this
    }

    fun frameBorderSize(frameBorderSize: Float): Theme {
        themeStyle[ThemeStyle.FrameBorderSize] = frameBorderSize
        return this
    }

    fun itemSpacing(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.ItemSpacing] = Vector2f(x, y)
        return this
    }

    fun itemInnerSpacing(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.ItemInnerSpacing] = Vector2f(x, y)
        return this
    }

    fun indentSpacing(indentSpacing: Float): Theme {
        themeStyle[ThemeStyle.IndentSpacing] = indentSpacing
        return this
    }

    fun cellPadding(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.CellPadding] = Vector2f(x, y)
        return this
    }

    fun scrollbarSize(scrollbarSize: Float): Theme {
        themeStyle[ThemeStyle.ScrollbarSize] = scrollbarSize
        return this
    }

    fun scrollbarRounding(scrollbarRounding: Float): Theme {
        themeStyle[ThemeStyle.ScrollbarRounding] = scrollbarRounding
        return this
    }

    fun grabMinSize(grabMinSize: Float): Theme {
        themeStyle[ThemeStyle.GrabMinSize] = grabMinSize
        return this
    }

    fun grabRounding(grabRounding: Float): Theme {
        themeStyle[ThemeStyle.GrabRounding] = grabRounding
        return this
    }

    fun tabRounding(tabRounding: Float): Theme {
        themeStyle[ThemeStyle.TabRounding] = tabRounding
        return this
    }

    fun buttonTextAlign(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.ButtonTextAlign] = Vector2f(x, y)
        return this
    }

    fun selectableTextAlign(x: Float, y: Float): Theme {
        themeStyle[ThemeStyle.SelectableTextAlign] = Vector2f(x, y)
        return this
    }

    fun text(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Text] = Vector4f(r, g, b, a)
        return this
    }

    fun textDisabled(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TextDisabled] = Vector4f(r, g, b, a)
        return this
    }

    fun windowBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.WindowBg] = Vector4f(r, g, b, a)
        return this
    }

    fun childBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ChildBg] = Vector4f(r, g, b, a)
        return this
    }

    fun popupBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.PopupBg] = Vector4f(r, g, b, a)
        return this
    }

    fun border(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Border] = Vector4f(r, g, b, a)
        return this
    }

    fun borderShadow(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.BorderShadow] = Vector4f(r, g, b, a)
        return this
    }

    fun frameBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.FrameBg] = Vector4f(r, g, b, a)
        return this
    }

    fun frameBgHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.FrameBgHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun frameBgActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.FrameBgActive] = Vector4f(r, g, b, a)
        return this
    }

    fun titleBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TitleBg] = Vector4f(r, g, b, a)
        return this
    }

    fun titleBgActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TitleBgActive] = Vector4f(r, g, b, a)
        return this
    }

    fun titleBgCollapsed(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TitleBgCollapsed] = Vector4f(r, g, b, a)
        return this
    }

    fun menuBarBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.MenuBarBg] = Vector4f(r, g, b, a)
        return this
    }

    fun scrollbarBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ScrollbarBg] = Vector4f(r, g, b, a)
        return this
    }

    fun scrollbarGrab(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ScrollbarGrab] = Vector4f(r, g, b, a)
        return this
    }

    fun scrollbarGrabHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ScrollbarGrabHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun scrollbarGrabActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ScrollbarGrabActive] = Vector4f(r, g, b, a)
        return this
    }

    fun checkMark(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.CheckMark] = Vector4f(r, g, b, a)
        return this
    }

    fun sliderGrab(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.SliderGrab] = Vector4f(r, g, b, a)
        return this
    }

    fun sliderGrabActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.SliderGrabActive] = Vector4f(r, g, b, a)
        return this
    }

    fun button(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Button] = Vector4f(r, g, b, a)
        return this
    }

    fun buttonHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ButtonHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun buttonActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ButtonActive] = Vector4f(r, g, b, a)
        return this
    }

    fun header(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Header] = Vector4f(r, g, b, a)
        return this
    }

    fun headerHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.HeaderHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun headerActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.HeaderActive] = Vector4f(r, g, b, a)
        return this
    }

    fun separator(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Separator] = Vector4f(r, g, b, a)
        return this
    }

    fun separatorHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.SeparatorHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun separatorActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.SeparatorActive] = Vector4f(r, g, b, a)
        return this
    }

    fun resizeGrip(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ResizeGrip] = Vector4f(r, g, b, a)
        return this
    }

    fun resizeGripHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ResizeGripHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun resizeGripActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ResizeGripActive] = Vector4f(r, g, b, a)
        return this
    }

    fun tab(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.Tab] = Vector4f(r, g, b, a)
        return this
    }

    fun tabHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TabHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun tabActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TabActive] = Vector4f(r, g, b, a)
        return this
    }

    fun tabUnfocused(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TabUnfocused] = Vector4f(r, g, b, a)
        return this
    }

    fun tabUnfocusedActive(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TabUnfocusedActive] = Vector4f(r, g, b, a)
        return this
    }

    fun dockingPreview(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.DockingPreview] = Vector4f(r, g, b, a)
        return this
    }

    fun dockingEmptyBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.DockingEmptyBg] = Vector4f(r, g, b, a)
        return this
    }

    fun plotLines(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.PlotLines] = Vector4f(r, g, b, a)
        return this
    }

    fun plotLinesHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.PlotLinesHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun plotHistogram(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.PlotHistogram] = Vector4f(r, g, b, a)
        return this
    }

    fun plotHistogramHovered(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.PlotHistogramHovered] = Vector4f(r, g, b, a)
        return this
    }

    fun tableHeaderBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TableHeaderBg] = Vector4f(r, g, b, a)
        return this
    }

    fun tableBorderStrong(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TableBorderStrong] = Vector4f(r, g, b, a)
        return this
    }

    fun tableBorderLight(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TableBorderLight] = Vector4f(r, g, b, a)
        return this
    }

    fun tableRowBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TableRowBg] = Vector4f(r, g, b, a)
        return this
    }

    fun tableRowBgAlt(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TableRowBgAlt] = Vector4f(r, g, b, a)
        return this
    }

    fun textSelectedBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.TextSelectedBg] = Vector4f(r, g, b, a)
        return this
    }

    fun dragDropTarget(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.DragDropTarget] = Vector4f(r, g, b, a)
        return this
    }

    fun navHighlight(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.NavHighlight] = Vector4f(r, g, b, a)
        return this
    }

    fun navWindowingHighlight(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.NavWindowingHighlight] = Vector4f(r, g, b, a)
        return this
    }

    fun navWindowingDimBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.NavWindowingDimBg] = Vector4f(r, g, b, a)
        return this
    }

    fun modalWindowDimBg(r: Float, g: Float, b: Float, a: Float): Theme {
        themeColors[ThemeColor.ModalWindowDimBg] = Vector4f(r, g, b, a)
        return this
    }


}


enum class ThemeStyle(val varId: Int) {
    Alpha(imgui.flag.ImGuiStyleVar.Alpha), DisabledAlpha(imgui.flag.ImGuiStyleVar.DisabledAlpha), WindowPadding(imgui.flag.ImGuiStyleVar.WindowPadding), WindowRounding(
        imgui.flag.ImGuiStyleVar.WindowRounding
    ),
    WindowBorderSize(imgui.flag.ImGuiStyleVar.WindowBorderSize), WindowMinSize(imgui.flag.ImGuiStyleVar.WindowMinSize), WindowTitleAlign(
        imgui.flag.ImGuiStyleVar.WindowTitleAlign
    ),
    ChildRounding(imgui.flag.ImGuiStyleVar.ChildRounding), ChildBorderSize(imgui.flag.ImGuiStyleVar.ChildBorderSize), PopupRounding(
        imgui.flag.ImGuiStyleVar.PopupRounding
    ),
    PopupBorderSize(imgui.flag.ImGuiStyleVar.PopupBorderSize), FramePadding(imgui.flag.ImGuiStyleVar.FramePadding), FrameRounding(
        imgui.flag.ImGuiStyleVar.FrameRounding
    ),
    FrameBorderSize(imgui.flag.ImGuiStyleVar.FrameBorderSize), ItemSpacing(imgui.flag.ImGuiStyleVar.ItemSpacing), ItemInnerSpacing(
        imgui.flag.ImGuiStyleVar.ItemInnerSpacing
    ),
    IndentSpacing(imgui.flag.ImGuiStyleVar.IndentSpacing), CellPadding(imgui.flag.ImGuiStyleVar.CellPadding), ScrollbarSize(
        imgui.flag.ImGuiStyleVar.ScrollbarSize
    ),
    ScrollbarRounding(imgui.flag.ImGuiStyleVar.ScrollbarRounding), GrabMinSize(imgui.flag.ImGuiStyleVar.GrabMinSize), GrabRounding(
        imgui.flag.ImGuiStyleVar.GrabRounding
    ),
    TabRounding(imgui.flag.ImGuiStyleVar.TabRounding), ButtonTextAlign(imgui.flag.ImGuiStyleVar.ButtonTextAlign), SelectableTextAlign(
        imgui.flag.ImGuiStyleVar.SelectableTextAlign
    );

    companion object {

        fun from(varId: Int): ThemeStyle {
            return entries.first { it.varId == varId }
        }
    }
}

enum class ThemeColor(val colorId: Int) {
    Text(ImGuiCol.Text), TextDisabled(ImGuiCol.TextDisabled), WindowBg(ImGuiCol.WindowBg), ChildBg(ImGuiCol.ChildBg), PopupBg(
        ImGuiCol.PopupBg
    ),
    Border(ImGuiCol.Border), BorderShadow(ImGuiCol.BorderShadow), FrameBg(ImGuiCol.FrameBg), FrameBgHovered(ImGuiCol.FrameBgHovered), FrameBgActive(
        ImGuiCol.FrameBgActive
    ),
    TitleBg(ImGuiCol.TitleBg), TitleBgActive(ImGuiCol.TitleBgActive), TitleBgCollapsed(ImGuiCol.TitleBgCollapsed), MenuBarBg(
        ImGuiCol.MenuBarBg
    ),
    ScrollbarBg(ImGuiCol.ScrollbarBg), ScrollbarGrab(ImGuiCol.ScrollbarGrab), ScrollbarGrabHovered(ImGuiCol.ScrollbarGrabHovered), ScrollbarGrabActive(
        ImGuiCol.ScrollbarGrabActive
    ),
    CheckMark(ImGuiCol.CheckMark), SliderGrab(ImGuiCol.SliderGrab), SliderGrabActive(ImGuiCol.SliderGrabActive), Button(
        ImGuiCol.Button
    ),
    ButtonHovered(ImGuiCol.ButtonHovered), ButtonActive(ImGuiCol.ButtonActive), Header(ImGuiCol.Header), HeaderHovered(
        ImGuiCol.HeaderHovered
    ),
    HeaderActive(ImGuiCol.HeaderActive), Separator(ImGuiCol.Separator), SeparatorHovered(ImGuiCol.SeparatorHovered), SeparatorActive(
        ImGuiCol.SeparatorActive
    ),
    ResizeGrip(ImGuiCol.ResizeGrip), ResizeGripHovered(ImGuiCol.ResizeGripHovered), ResizeGripActive(ImGuiCol.ResizeGripActive), Tab(
        ImGuiCol.Tab
    ),
    TabHovered(ImGuiCol.TabHovered), TabActive(ImGuiCol.TabActive), TabUnfocused(ImGuiCol.TabUnfocused), TabUnfocusedActive(
        ImGuiCol.TabUnfocusedActive
    ),
    DockingPreview(ImGuiCol.DockingPreview), DockingEmptyBg(ImGuiCol.DockingEmptyBg), PlotLines(ImGuiCol.PlotLines), PlotLinesHovered(
        ImGuiCol.PlotLinesHovered
    ),
    PlotHistogram(ImGuiCol.PlotHistogram), PlotHistogramHovered(ImGuiCol.PlotHistogramHovered), TableHeaderBg(ImGuiCol.TableHeaderBg), TableBorderStrong(
        ImGuiCol.TableBorderStrong
    ),
    TableBorderLight(ImGuiCol.TableBorderLight), TableRowBg(ImGuiCol.TableRowBg), TableRowBgAlt(ImGuiCol.TableRowBgAlt), TextSelectedBg(
        ImGuiCol.TextSelectedBg
    ),
    DragDropTarget(ImGuiCol.DragDropTarget), NavHighlight(ImGuiCol.NavHighlight), NavWindowingHighlight(ImGuiCol.NavWindowingHighlight), NavWindowingDimBg(
        ImGuiCol.NavWindowingDimBg
    ),
    ModalWindowDimBg(ImGuiCol.ModalWindowDimBg);

    companion object {

        fun from(color: Int): ThemeColor {
            return entries.first { it.colorId == color }
        }
    }
}


fun loadFromResources(file: String): ByteArray {
    return try {
        Files.readAllBytes(
            Paths.get(
                Theme::class.java.getResource(file)?.toURI()
                    ?: throw RuntimeException("Could not load resource $file")
            )
        )
    } catch (e: IOException) {
        throw RuntimeException(e)
    } catch (e: URISyntaxException) {
        throw RuntimeException(e)
    }
}