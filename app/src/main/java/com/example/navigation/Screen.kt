package com.example.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object Transactions : Screen("transactions")
    object SplitBill : Screen("split_bill")
    object Analytics : Screen("analytics")
    object AiCoach : Screen("ai_coach")
    object Settings : Screen("settings")
    object SavingsGoals : Screen("savings_goals")
    object Accounts : Screen("accounts")
    object AutomaticRules : Screen("automatic_rules")
    object RecurringTransactions : Screen("recurring_transactions")
    object PremiumPaywall : Screen("premium_paywall")
    object SavingsGoalDetail : Screen("savings_goal_detail/{goalId}") {
        fun createRoute(goalId: Int) = "savings_goal_detail/$goalId"
    }
}
