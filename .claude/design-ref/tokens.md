# AI Study Mentor — Design Tokens

Source: `colors.xml`, `dimens.xml`, `themes.xml`

---

## Colors

### Brand
| Token | Value | Role |
|---|---|---|
| `brand_primary` | `#F5B544` | CTA buttons, icon backgrounds, amber fills |
| `brand_primary_soft` | `#FCEDC0` | Chips, containers, warning_soft |
| `brand_primary_tint` | `#FEF8E7` | Bottom nav indicator, light washes |
| `brand_primary_deep` | `#C7800A` | Pressed / hover state |
| `brand_primary_darker` | `#8E5A06` | On-primary-container text |
| `brand_accent` | `#E47B47` | Secondary CTA, orange accent |
| `brand_accent_soft` | `#FBE2D2` | Orange tint backgrounds |

### Neutrals
| Token | Value | Role |
|---|---|---|
| `bg` | `#FAF5EA` | Page / screen background (warm cream) |
| `surface` | `#FFFFFF` | Cards, bottom sheets, nav bar |
| `surface_2` | `#F4ECD8` | Slightly elevated surface |
| `surface_3` | `#EDE3CB` | More elevated surface |
| `text_primary` | `#2A2418` | Headings, body copy |
| `text_secondary` | `#6B5F47` | Labels, captions |
| `text_tertiary` | `#A39479` | Placeholders, metadata, disabled |
| `text_on_primary` | `#FFFFFF` | Text on amber fills |
| `border` | `#E5D9BC` | Card strokes, dividers |
| `border_strong` | `#D4C49E` | Emphasized separators |

### Semantic
| Token | Value |
|---|---|
| `success` | `#6FA84B` |
| `success_soft` | `#E4F0D7` |
| `warning` | `#E69D0F` |
| `warning_soft` | `#FCEDC0` |
| `error` | `#D4624B` |
| `error_soft` | `#FAE0D8` |
| `info` | `#4A8BA8` |
| `info_soft` | `#DCEDF3` |

### Subject Palette
| Subject | Solid | Soft bg |
|---|---|---|
| math | `#7C5CE6` | `#EFEAFE` |
| science | `#4FA37A` | `#E1F0E8` |
| code | `#3A86D9` | `#DCEAF8` |
| history | `#D4624B` | `#FAE0D8` |
| language | `#E47B47` | `#FBE2D2` |
| geo | `#4A8BA8` | `#DCEDF3` |

---

## Spacing (4 dp grid)
| Token | Value |
|---|---|
| `space_1` | 4 dp |
| `space_2` | 8 dp |
| `space_3` | 12 dp |
| `space_4` | 16 dp |
| `space_5` | 20 dp |
| `space_6` | 24 dp |
| `space_8` | 32 dp |
| `space_10` | 40 dp |
| `space_12` | 48 dp |

---

## Corner Radius
| Token | Value | Used on |
|---|---|---|
| `radius_xs` | 6 dp | Small chips, tags |
| `radius_sm` | 10 dp | Small components (Material S) |
| `radius_md` | 14 dp | Cards (Material M) |
| `radius_lg` | 20 dp | Buttons, large cards (Material L) |
| `radius_xl` | 28 dp | Bottom sheets |
| `radius_pill` | 999 dp | Fully-rounded / FABs |

---

## Typography
| Style | Size | Weight | Color |
|---|---|---|---|
| Display | 32 sp | Bold | `text_primary`, letter-spacing −0.02 em |
| H1 | 26 sp | Bold | `text_primary` |
| H2 | 22 sp | Bold | `text_primary` |
| H3 | 18 sp | Bold | `text_primary` |
| Body | 15 sp | Regular | `text_primary` |
| Body SM | 14 sp | Regular | `text_primary` |
| Caption | 13 sp | Regular | `text_secondary` |
| Micro | 11 sp | Regular | — |

**Font family:** Roboto (Material 3 system default — no custom typeface declared)

---

## Component Dimensions
| Token | Value | Used on |
|---|---|---|
| `button_height_sm` | 36 dp | Small buttons |
| `button_height_md` | 48 dp | Default buttons |
| `button_height_lg` | 56 dp | Large buttons |
| `input_height` | 52 dp | Text input fields |
| `icon_button_sm` | 36 dp | Small icon buttons |
| `icon_button_md` | 40 dp | Medium icon buttons |
| `icon_button_lg` | 44 dp | Large icon buttons |
| `appbar_height` | 56 dp | Top app bar |
| `bottomnav_height` | 72 dp | Bottom navigation bar |
| `touch_min` | 48 dp | Minimum touch target |

### Elevation
| Token | Value |
|---|---|
| `elev_1` | 2 dp |
| `elev_2` | 6 dp |
| `elev_3` | 12 dp |

### Mascot (Milo)
| Token | Value |
|---|---|
| `mascot_xs` | 28 dp |
| `mascot_sm` | 40 dp |
| `mascot_md` | 72 dp |
| `mascot_lg` | 120 dp |

---

## Component Style Recipes (from themes.xml)

### Button — Primary
- Fill: `brand_primary` (`#F5B544`)
- Text: `text_on_primary` (`#FFFFFF`), bold, 15 sp
- Height: 48 dp · Corner: `radius_lg` (20 dp) · Elevation: `elev_1` (2 dp)

### Button — Secondary (outlined)
- Fill: `surface` (`#FFFFFF`)
- Stroke: `border` (`#E5D9BC`), 1 dp
- Text: `text_primary` (`#2A2418`)
- Height: 48 dp · Corner: `radius_lg` (20 dp)

### Card — Default
- Fill: `surface` (`#FFFFFF`)
- Stroke: `border` (`#E5D9BC`), 1 dp
- Corner: `radius_md` (14 dp) · Elevation: 0 dp

### Chip — Filter
- Fill: `surface` (`#FFFFFF`)
- Stroke: `border` (`#E5D9BC`), 1 dp
- Text: `text_primary`

### Shape Scale (Material 3 overrides)
| Level | Radius |
|---|---|
| Small components | 10 dp (`radius_sm`) |
| Medium components | 14 dp (`radius_md`) |
| Large components | 20 dp (`radius_lg`) |
| Round | 50 % |