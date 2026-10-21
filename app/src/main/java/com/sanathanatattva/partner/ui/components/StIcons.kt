package com.sanathanatattva.partner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons copied from the canvas SVGs (24-unit grid). Tint them with Icon(tint = …). */
object StIcons {
    val ArrowRight by lazy { line("arrow_right", 2.6f, "M5 12h14", "M13 6l6 6-6 6") }
    val Mail by lazy { line("mail", 2f, "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M4 7l8 6 8-6") }
    val Lock by lazy { line("lock", 2f, "M7 10h10a3 3 0 0 1 3 3v5a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-5a3 3 0 0 1 3-3z", "M8 10V7a4 4 0 0 1 8 0v3") }
    val Eye by lazy { line("eye", 2f, "M2.5 12C3.5 10 7 6 12 6s8.5 4 9.5 6c-1 2-4.5 6-9.5 6s-8.5-4-9.5-6z", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0") }
    val EyeOff by lazy {
        line(
            "eye_off", 2f, "M3 3l18 18",
            "M10.6 6.1A9.7 9.7 0 0 1 12 6c5 0 8.5 4 9.5 6a13 13 0 0 1-2.6 3.4M6.3 7.6A13 13 0 0 0 2.5 12c1 2 4.5 6 9.5 6a9 9 0 0 0 4.2-1",
            "M9.9 9.9a3 3 0 0 0 4.2 4.2",
        )
    }
    val Check by lazy { line("check", 3.2f, "M5 12.5l4.5 4.5L19 7.5") }
    val Phone by lazy { line("phone", 2.2f, "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2") }
    val Speaker by lazy { line("speaker", 2f, "M4 9.5h3.5L12 5.5v13l-4.5-4H4z", "M15.5 9a4 4 0 0 1 0 6") }
    val Gear by lazy {
        line(
            "gear", 2f, "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        )
    }

    val ArrowLeft by lazy { line("arrow_left", 2.4f, "M19 12H5", "M11 6l-6 6 6 6") }
    val Move by lazy { line("move", 2f, "M12 3v18M3 12h18M12 3l-3 3M12 3l3 3M12 21l-3-3M12 21l3-3M3 12l3-3M3 12l3 3M21 12l-3-3M21 12l-3 3") }
    val Locate by lazy { line("locate", 2.2f, "M5.5 12a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0-13 0", "M12 2v3M12 19v3M2 12h3M19 12h3", "M9.8 12a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0-4.4 0") }
    val Home by lazy { line("home", 2.2f, "M4 11l8-7 8 7", "M6 9.5V20h12V9.5", "M10 20v-5h4v5") }
    val Shop by lazy { line("shop", 2.2f, "M4 9l1.5-5h13L20 9", "M4 9a2.7 2.7 0 0 0 5.3 0 2.7 2.7 0 0 0 5.4 0A2.7 2.7 0 0 0 20 9", "M5.5 11.5V20h13v-8.5", "M10 20v-5h4v5") }
    val Pin by lazy { line("pin", 2.2f, "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", "M9.4 9.5a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0") }
    val Plus by lazy { line("plus", 2.4f, "M12 5v14M5 12h14") }
    val Download by lazy { line("download", 2.2f, "M12 15V3", "M7 10l5 5 5-5", "M5 21h14") }
    val Info by lazy { line("info", 2.2f, "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 8v5", "M12 16h.01") }
    val Sun by lazy { line("sun", 2f, "M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0", "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4") }
    val NoHelmet by lazy { line("no_helmet", 2f, "M4 15a8 8 0 0 1 16 0z", "M2 15h20", "M3 3l18 18") }
    val NoGlasses by lazy { line("no_glasses", 2f, "M3 14a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0", "M14 14a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0", "M10 14h4", "M3 3l18 18") }
    val Camera by lazy { line("camera", 2.2f, "M4 8h3l2-3h6l2 3h3v11H4z", "M8.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0") }
    val Retake by lazy { line("retake", 2.2f, "M4 12a8 8 0 0 1 14-5.3L20 9", "M20 4v5h-5", "M20 12a8 8 0 0 1-14 5.3L4 15", "M4 20v-5h5") }
    val IdCard by lazy { line("id_card", 2f, "M5.5 5h13a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3h-13a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M6.3 11a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0-4.4 0", "M5.5 16a3 3 0 0 1 6 0", "M14 10h4.5M14 13.5h3") }
    val Bank by lazy { line("bank", 2f, "M3 9.5L12 4l9 5.5", "M5 10v8M9.5 10v8M14.5 10v8M19 10v8", "M3 20.5h18") }
    val Shield by lazy { line("shield", 2f, "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z", "M9 12l2 2 4-4") }
    val Contacts by lazy { line("contacts", 2f, "M7 3h9a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M8.7 10a2.8 2.8 0 1 0 5.6 0a2.8 2.8 0 1 0-5.6 0", "M7.5 17a4 4 0 0 1 8 0", "M19 7h2M19 12h2M19 17h2") }
    val Heart by lazy { line("heart", 2f, "M12 20.5s-8-4.8-8-10.5a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.7-8 10.5-8 10.5z") }
    val Send by lazy { line("send", 2.2f, "M21 3L10.5 13.5", "M21 3l-6.5 18-4-7.5L3 9.5z") }
    val Edit by lazy { line("edit", 2f, "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4") }
    val Walk by lazy { line("walk", 3f, 64f, "M28.5 10a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0-11 0", "M32 19L28 36M31 23l-8 8M31 23l8 8M28 36l6 10 2 10M28 36l-4 11-6 8") }
    val Cycle by lazy { line("cycle", 3f, 64f, "M5 44a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M39 44a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M15 44L26 26L31 44Z M26 26H42L31 44 M42 26L49 44 M42 26L40 18H46 M22 23H30") }
    val Scooter by lazy { line("scooter", 3f, 64f, "M9 47a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M43 47a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M8 41Q8 31 18 31H30L34 39H42 M44 17L50 40 M40 16H48 M14 26H27") }
    val Bike by lazy { line("bike", 3f, 64f, "M4 44a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M40 44a10 10 0 1 0 20 0a10 10 0 1 0-20 0", "M14 44L24 32H38 M38 32L44 22H49 M44 22L50 44 M24 32L30 42H38L42 32 M20 27H32") }

    val CanNoNew by lazy { line("can_no_new", 2.4f, 32f, "M11 10h10a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H11a3 3 0 0 1-3-3V13a3 3 0 0 1 3-3z", "M12 10q4-5 8 0", "M4 5l24 23") }
    val CanNew by lazy { line("can_new", 2.4f, 32f, "M11 10h10a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H11a3 3 0 0 1-3-3V13a3 3 0 0 1 3-3z", "M12 10q4-5 8 0", "M16 15v8M12 19h8") }
    val Person by lazy { line("person", 2.2f, "M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0", "M4 21a8 8 0 0 1 16 0") }
    val Power by lazy { line("power", 2.6f, "M12 3.5v8", "M7 6.5a7.5 7.5 0 1 0 10 0") }
    val Bell by lazy { line("bell", 2.2f, "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0") }
    val Battery by lazy { line("battery", 2.2f, "M5.5 7h11a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 3 14.5v-5A2.5 2.5 0 0 1 5.5 7z", "M21 10.5v3", "M11.5 9l-2 3.2h3l-2 3.3") }
    val PhoneRing by lazy { line("phone_ring", 2.2f, "M9.5 3h5A2.5 2.5 0 0 1 17 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-5A2.5 2.5 0 0 1 7 18.5v-13A2.5 2.5 0 0 1 9.5 3z", "M11 18h2", "M3.5 9a5 5 0 0 0 0 6M20.5 9a5 5 0 0 1 0 6") }
    val Moon by lazy { line("moon", 2.2f, "M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z", "M4 4l16 16") }
    val Phone2 by lazy { line("phone_plain", 2.2f, "M9.5 3h5A2.5 2.5 0 0 1 17 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-5A2.5 2.5 0 0 1 7 18.5v-13A2.5 2.5 0 0 1 9.5 3z", "M11 18h2") }
    val Headset by lazy { line("headset", 2.2f, "M4 14v-2a8 8 0 0 1 16 0v2", "M4.5 13h1A1.5 1.5 0 0 1 7 14.5v3A1.5 1.5 0 0 1 5.5 19h-1A1.5 1.5 0 0 1 3 17.5v-3A1.5 1.5 0 0 1 4.5 13z", "M18.5 13h1a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-3a1.5 1.5 0 0 1 1.5-1.5z", "M19 19c0 1.5-2 2.5-5 2.5") }
    val ChevronRight by lazy { line("chevron_right", 2.4f, "M9 6l6 6-6 6") }
    val Clock by lazy { line("clock", 2.2f, "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 7v5l3 2") }
    val Language by lazy { line("language", 2f, "M4 5h9M8.5 3v2M6 5c0 4 2.5 7 6 8M11 5c0 3-2.5 7-7 9", "M13 21l4-9 4 9M14.5 18h5") }
    val Copy by lazy { line("copy", 2.2f, "M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8z", "M16 8V5.5A1.5 1.5 0 0 0 14.5 4h-9A1.5 1.5 0 0 0 4 5.5v9A1.5 1.5 0 0 0 5.5 16H8") }
    val Hand by lazy { line("hand", 2.4f, "M8 13V5.5a1.5 1.5 0 0 1 3 0V12", "M11 11.5V4a1.5 1.5 0 0 1 3 0v8", "M14 12V6a1.5 1.5 0 0 1 3 0v8.5c0 3.6-2.6 6.5-6 6.5-2.4 0-4.2-1.3-5.3-3.3L3.6 13.6a1.4 1.4 0 0 1 2.2-1.7L8 14") }
    val Toggle by lazy { line("toggle", 2.2f, "M7 7h10a5 5 0 0 1 0 10H7A5 5 0 0 1 7 7z", "M4 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0") }
    val CircleCheck by lazy { line("circle-check", 2.6f, "M2.5 12a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0", "M7.5 12.3l3 3 6-6.3") }
    val Image by lazy { line("image", 2.2f, "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M7.4 10a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0", "M21 16l-5-5-7 8") }
    val Map by lazy { line("map", 2.2f, "M3 6.5l6-2.5 6 2.5 6-2.5v13.5l-6 2.5-6-2.5-6 2.5z", "M9 4v13.5M15 6.5V20") }
    val Backspace by lazy { line("backspace", 2.2f, "M9 5h11v14H9l-6-7z", "M12.5 9.5l5 5M17.5 9.5l-5 5") }
    val Keypad by lazy { line("keypad", 2.2f, "M6 6h12a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z", "M7 12h.01M11 12h.01M15 12h.01") }
    val Arrived by lazy { line("arrived", 2.4f, "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", "M9 9.5l2 2 4-4") }
    val NotHome by lazy { line("not_home", 2.2f, "M4 11l8-7 8 7", "M6 9.5V20h12V9.5", "M10 13l4 4M14 13l-4 4") }
    val PhoneOff by lazy { line("phone_off", 2.2f, "M9.5 3h5A2.5 2.5 0 0 1 17 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-5A2.5 2.5 0 0 1 7 18.5v-13A2.5 2.5 0 0 1 9.5 3z", "M3 3l18 18") }
    val PinUnsure by lazy { line("pin_unsure", 2.2f, "M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", "M10.3 8.2a1.9 1.9 0 1 1 2.6 1.8c-.6.3-.9.7-.9 1.3", "M12 13.4h.01") }
    val Ban by lazy { line("ban", 2.2f, "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M5.6 5.6l12.8 12.8") }
    val Dots by lazy { line("dots", 4f, "M6 12h.01M12 12h.01M18 12h.01") }
    val Refresh by lazy { line("refresh", 2.4f, "M20 12a8 8 0 1 1-2.3-5.6", "M20 4v5h-5") }
    val Search by lazy { line("search", 3f, "M5 11a6 6 0 1 0 12 0a6 6 0 1 0-12 0", "M16 16l4 4") }
    val ChevronLeft by lazy { line("chevron_left", 2.4f, "M15 6l-6 6 6 6") }
    val DoubleChevronRight by lazy { line("double_chevron_right", 3f, "M4 6l6 6-6 6", "M12 6l6 6-6 6") }
    val DoubleChevronLeft by lazy { line("double_chevron_left", 3f, "M20 6l-6 6 6 6", "M12 6l-6 6 6 6") }
    val Rupee by lazy { line("rupee", 2.2f, "M6 3h12", "M6 8h12", "M6 13h3", "M9 13c6.667 0 6.667-10 0-10", "M6 13l8.5 8") }

    // My Shop (canvas "ST Delivery · My Shop")
    val WhatsApp by lazy { line("whatsapp", 1.9f, "M3.5 20.5l1.4-4.1A8.5 8.5 0 1 1 8 19.3z", "M9 9.5c.3 2.3 2.2 4.3 4.6 4.8l1.2-1.3 1.9.9-.3 1.6c-3.9.3-8-3.6-7.8-7.4l1.6-.4.9 1.9z") }
    val Store by lazy { line("store", 1.9f, "M4 9h16l-1.6-5H5.6z", "M5 9v11h14V9", "M10 20v-5h4v5") }
    val Truck by lazy { line("truck", 1.9f, "M2.5 6.5h11v10h-11z", "M13.5 9.5h4l3 3.2v3.8h-7", "M4.5 17.5a2 2 0 1 0 4 0a2 2 0 1 0-4 0", "M14.5 17.5a2 2 0 1 0 4 0a2 2 0 1 0-4 0") }
    val People by lazy { line("people", 1.9f, "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0", "M2.5 20c0-3.5 3-5.5 6.5-5.5s6.5 2 6.5 5.5", "M14.5 9a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M16.5 14.6c2.8.2 5 1.8 5 4.9") }
    val Minus by lazy { line("minus", 2.4f, "M5 12h14") }
    val Share by lazy { line("share", 2f, "M15.5 5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M3.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M15.5 19a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", "M8.2 10.8l7.6-4.4M8.2 13.2l7.6 4.4") }
    val Trophy by lazy { line("trophy", 2f, "M7 4h10v4a5 5 0 0 1-10 0z", "M7 6H4a3 3 0 0 0 3 4M17 6h3a3 3 0 0 1-3 4", "M12 13v4M8 21h8M9.5 17h5") }
    val WifiOff by lazy { line("wifi_off", 2f, "M2 8.5a15 15 0 0 1 5-3M22 8.5A15 15 0 0 0 11 4.6M5 12a10 10 0 0 1 3.5-2.2M19 12a10 10 0 0 0-4-2.4M8.5 15.5a5 5 0 0 1 7 0", "M11 19a1 1 0 1 0 2 0a1 1 0 1 0-2 0", "M3 3l18 18") }

    private fun line(name: String, width: Float, vararg paths: String): ImageVector = line(name, width, 24f, *paths)

    private fun line(name: String, width: Float, viewport: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, viewport, viewport).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
