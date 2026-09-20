// SPDX-FileCopyrightText: 2026 SBRO
// SPDX-License-Identifier: LicenseRef-EmuCoreA-Proprietary
package com.sbro.emucorea.core

import androidx.annotation.StringRes
import com.sbro.emucorea.R

/**
 * Help hints for the built-in PPSSPP core options. The hint text itself lives
 * in the strings.xml resources of every supported locale like every other
 * setting hint; this table only maps the stable option key to its resource.
 */
@StringRes
fun ppssppCoreOptionHelpRes(key: String): Int? = when (key) {
    "ppsspp_frameskip" -> R.string.settings_help_core_frameskip
    "ppsspp_frameskiptype" -> R.string.settings_help_core_frameskiptype
    "ppsspp_auto_frameskip" -> R.string.settings_help_core_auto_frameskip
    "ppsspp_gpu_hardware_transform" -> R.string.settings_help_core_gpu_hardware_transform
    "ppsspp_texture_scaling_type" -> R.string.settings_help_core_texture_scaling_type
    "ppsspp_texture_anisotropic_filtering" -> R.string.settings_help_core_texture_anisotropic_filtering
    "ppsspp_texture_filtering" -> R.string.settings_help_core_texture_filtering
    "ppsspp_texture_replacement" -> R.string.settings_help_core_texture_replacement
    else -> null
}
