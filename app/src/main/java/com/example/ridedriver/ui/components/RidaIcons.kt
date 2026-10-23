package com.example.ridedriver.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.composables.icons.lucide.R as LucideR

/** Semantic icon catalog. All UI glyphs here are Lucide Android vector drawables. */
object RidaIcons {
    val Home = LucideR.drawable.lucide_ic_house
    val Rides = LucideR.drawable.lucide_ic_car_front
    val Account = LucideR.drawable.lucide_ic_user_round
    val Wallet = LucideR.drawable.lucide_ic_wallet
    val Payment = LucideR.drawable.lucide_ic_credit_card
    val History = LucideR.drawable.lucide_ic_history
    val SavedPlace = LucideR.drawable.lucide_ic_map_pin
    val Pickup = LucideR.drawable.lucide_ic_map_pinned
    val Navigation = LucideR.drawable.lucide_ic_navigation
    val Promotions = LucideR.drawable.lucide_ic_ticket_percent
    val Notifications = LucideR.drawable.lucide_ic_bell
    val Settings = LucideR.drawable.lucide_ic_settings_2
    val Support = LucideR.drawable.lucide_ic_headset
    val HelpArticles = LucideR.drawable.lucide_ic_book_open
    val Safety = LucideR.drawable.lucide_ic_shield_check
    val Verified = LucideR.drawable.lucide_ic_badge_check
    val Logout = LucideR.drawable.lucide_ic_log_out
    val EditProfile = LucideR.drawable.lucide_ic_user_round_pen

    val Dashboard = LucideR.drawable.lucide_ic_layout_dashboard
    val Route = LucideR.drawable.lucide_ic_route
    val Earnings = LucideR.drawable.lucide_ic_chart_column_increasing
    val Payout = LucideR.drawable.lucide_ic_banknote
    val Vehicle = LucideR.drawable.lucide_ic_car
    val DriverLicence = LucideR.drawable.lucide_ic_id_card
    val Document = LucideR.drawable.lucide_ic_file_text
    val Upload = LucideR.drawable.lucide_ic_upload
    val Pending = LucideR.drawable.lucide_ic_clock_3

    val Back = LucideR.drawable.lucide_ic_arrow_left
    val Forward = LucideR.drawable.lucide_ic_arrow_right
    val ChevronRight = LucideR.drawable.lucide_ic_chevron_right
    val Check = LucideR.drawable.lucide_ic_check
    val Add = LucideR.drawable.lucide_ic_plus
    val Attachment = LucideR.drawable.lucide_ic_paperclip
    val Calendar = LucideR.drawable.lucide_ic_calendar_days
    val Chat = LucideR.drawable.lucide_ic_message_circle
    val Edit = LucideR.drawable.lucide_ic_pencil
    val Mail = LucideR.drawable.lucide_ic_mail
    val Heart = LucideR.drawable.lucide_ic_heart
    val List = LucideR.drawable.lucide_ic_list
    val Help = LucideR.drawable.lucide_ic_circle_question_mark
    val Info = LucideR.drawable.lucide_ic_info
    val Language = LucideR.drawable.lucide_ic_globe
    val Lock = LucideR.drawable.lucide_ic_lock
    val Map = LucideR.drawable.lucide_ic_map
    val Phone = LucideR.drawable.lucide_ic_phone
    val Search = LucideR.drawable.lucide_ic_search
    val ShowPassword = LucideR.drawable.lucide_ic_eye
    val HidePassword = LucideR.drawable.lucide_ic_eye_off
    val Sound = LucideR.drawable.lucide_ic_volume_2
    val Work = LucideR.drawable.lucide_ic_briefcase_business
    val Close = LucideR.drawable.lucide_ic_x
    val Star = LucideR.drawable.lucide_ic_star
    val Users = LucideR.drawable.lucide_ic_users
    val Package = LucideR.drawable.lucide_ic_package
    val MobileMoney = LucideR.drawable.lucide_ic_smartphone
    val Cash = LucideR.drawable.lucide_ic_banknote
    val Locate = LucideR.drawable.lucide_ic_locate_fixed
    val Warning = LucideR.drawable.lucide_ic_triangle_alert
    val Share = LucideR.drawable.lucide_ic_share_2
    val Minus = LucideR.drawable.lucide_ic_minus
    val Leaf = LucideR.drawable.lucide_ic_leaf
}

/** Keeps the existing Compose icon layout and tint behavior while using Lucide resources. */
@Composable
fun RidaIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl &&
        (icon == RidaIcons.Back || icon == RidaIcons.Forward ||
            icon == RidaIcons.ChevronRight || icon == RidaIcons.Logout)
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = if (mirror) modifier.scale(scaleX = -1f, scaleY = 1f) else modifier,
        tint = tint,
    )
}
