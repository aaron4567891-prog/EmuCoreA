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
    "ppsspp_cpu_core" -> R.string.settings_help_core_cpu_core
    "ppsspp_ignore_bad_memory_access" -> R.string.settings_help_core_ignore_bad_memory_access
    "ppsspp_io_timing_method" -> R.string.settings_help_core_io_timing_method
    "ppsspp_locked_cpu_speed" -> R.string.settings_help_core_locked_cpu_speed
    "ppsspp_memstick_size" -> R.string.settings_help_core_memstick_size
    "ppsspp_cache_iso" -> R.string.settings_help_core_cache_iso
    "ppsspp_cheats" -> R.string.settings_help_core_cheats
    "ppsspp_psp_model" -> R.string.settings_help_core_psp_model
    "ppsspp_button_preference" -> R.string.settings_help_core_button_preference
    "ppsspp_analog_is_circular" -> R.string.settings_help_core_analog_is_circular
    else -> null
}
