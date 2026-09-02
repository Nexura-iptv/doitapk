package com.example.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.model.AvailablePlans
import com.example.data.model.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_ai_subscription_prefs", Context.MODE_PRIVATE)

    private val _currentPlan = MutableStateFlow(loadSavedPlan())
    val currentPlan: StateFlow<SubscriptionPlan> = _currentPlan.asStateFlow()

    private fun loadSavedPlan(): SubscriptionPlan {
        val planId = prefs.getString(KEY_ACTIVE_PLAN_ID, AvailablePlans.defaultPlan.id) ?: AvailablePlans.defaultPlan.id
        return AvailablePlans.findById(planId)
    }

    fun activatePlan(plan: SubscriptionPlan) {
        prefs.edit()
            .putString(KEY_ACTIVE_PLAN_ID, plan.id)
            .putLong(KEY_PLAN_ACTIVATED_AT, System.currentTimeMillis())
            .apply()
        _currentPlan.value = plan
    }

    fun openWebsite(plan: SubscriptionPlan? = null) {
        val url = plan?.websiteUrl ?: "https://brandofpaper.ikas.shop"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val KEY_ACTIVE_PLAN_ID = "active_plan_id"
        private const val KEY_PLAN_ACTIVATED_AT = "plan_activated_at"
    }
}
