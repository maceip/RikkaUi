package zed.rainxch.rikkaui.components.ui.icon

import zed.rainxch.rikkaicons.core.IconToken
// The tokens are extension properties on the pack's RikkaIcons object, declared
// across category files, so they must be imported individually to be in scope.
import zed.rainxch.rikkaicons.tokens.ArrowDown
import zed.rainxch.rikkaicons.tokens.ArrowLeft
import zed.rainxch.rikkaicons.tokens.ArrowRight
import zed.rainxch.rikkaicons.tokens.ArrowUp
import zed.rainxch.rikkaicons.tokens.Check
import zed.rainxch.rikkaicons.tokens.ChevronDown
import zed.rainxch.rikkaicons.tokens.ChevronLeft
import zed.rainxch.rikkaicons.tokens.ChevronRight
import zed.rainxch.rikkaicons.tokens.ChevronUp
import zed.rainxch.rikkaicons.tokens.Copy
import zed.rainxch.rikkaicons.tokens.Download
import zed.rainxch.rikkaicons.tokens.Edit
import zed.rainxch.rikkaicons.tokens.Eye
import zed.rainxch.rikkaicons.tokens.Heart
import zed.rainxch.rikkaicons.tokens.Home
import zed.rainxch.rikkaicons.tokens.Mail
import zed.rainxch.rikkaicons.tokens.Menu
import zed.rainxch.rikkaicons.tokens.Mic
import zed.rainxch.rikkaicons.tokens.Minus
import zed.rainxch.rikkaicons.tokens.Moon
import zed.rainxch.rikkaicons.tokens.MoreHorizontal
import zed.rainxch.rikkaicons.tokens.MoreVertical
import zed.rainxch.rikkaicons.tokens.Phone
import zed.rainxch.rikkaicons.tokens.Plus
import zed.rainxch.rikkaicons.tokens.Search
import zed.rainxch.rikkaicons.tokens.Send
import zed.rainxch.rikkaicons.tokens.Settings
import zed.rainxch.rikkaicons.tokens.Star
import zed.rainxch.rikkaicons.tokens.Sun
import zed.rainxch.rikkaicons.tokens.Trash
import zed.rainxch.rikkaicons.tokens.Upload
import zed.rainxch.rikkaicons.tokens.User
import zed.rainxch.rikkaicons.tokens.X
import zed.rainxch.rikkaicons.tokens.RikkaIcons as SemanticIcons

/**
 * RikkaUI's stable semantic icon surface.
 *
 * These values intentionally contain no vector paths. [Icon] resolves each
 * token through the ambient RikkaIcons pack, which RikkaUI configures as
 * Phosphor at the application root.
 */
public object RikkaIcons {
    public val Home: IconToken = SemanticIcons.Home
    public val ChevronRight: IconToken = SemanticIcons.ChevronRight
    public val ChevronDown: IconToken = SemanticIcons.ChevronDown
    public val ChevronLeft: IconToken = SemanticIcons.ChevronLeft
    public val ChevronUp: IconToken = SemanticIcons.ChevronUp
    public val Check: IconToken = SemanticIcons.Check
    public val X: IconToken = SemanticIcons.X
    public val Plus: IconToken = SemanticIcons.Plus
    public val Minus: IconToken = SemanticIcons.Minus
    public val Search: IconToken = SemanticIcons.Search
    public val ArrowLeft: IconToken = SemanticIcons.ArrowLeft
    public val ArrowRight: IconToken = SemanticIcons.ArrowRight
    public val ArrowUp: IconToken = SemanticIcons.ArrowUp
    public val ArrowDown: IconToken = SemanticIcons.ArrowDown
    public val Menu: IconToken = SemanticIcons.Menu
    public val MoreHorizontal: IconToken = SemanticIcons.MoreHorizontal
    public val MoreVertical: IconToken = SemanticIcons.MoreVertical
    public val Mail: IconToken = SemanticIcons.Mail
    public val Send: IconToken = SemanticIcons.Send
    public val User: IconToken = SemanticIcons.User
    public val Heart: IconToken = SemanticIcons.Heart
    public val Star: IconToken = SemanticIcons.Star
    public val Eye: IconToken = SemanticIcons.Eye
    public val Copy: IconToken = SemanticIcons.Copy
    public val Trash: IconToken = SemanticIcons.Trash
    public val Edit: IconToken = SemanticIcons.Edit
    public val Download: IconToken = SemanticIcons.Download
    public val Upload: IconToken = SemanticIcons.Upload
    public val Sun: IconToken = SemanticIcons.Sun
    public val Moon: IconToken = SemanticIcons.Moon
    public val Phone: IconToken = SemanticIcons.Phone
    public val Mic: IconToken = SemanticIcons.Mic
    public val Settings: IconToken = SemanticIcons.Settings
}
