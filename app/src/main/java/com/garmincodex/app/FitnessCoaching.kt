package com.garmincodex.app

internal data class FitnessSuggestion(val factor:String,val title:String,val action:String,val why:String)
internal object FitnessCoaching {
    fun focus(result:FitnessAgeResult,currentRecovery:Int?=null):Int {
        if(result.factors.isEmpty()) return 0
        val recovery=result.factors.indexOfFirst{it.name=="Recovery" && (it.score<34 || currentRecovery!=null && currentRecovery<34)}
        return if(recovery>=0) recovery else result.factors.indices.minBy{result.factors[it].score}
    }
    fun suggestion(factor:FitnessFactor,lowRecovery:Boolean):FitnessSuggestion = when(factor.name) {
        "Daily movement" -> FitnessSuggestion(factor.name,"Make movement easier to repeat","Try a short, comfortable walk or movement break during a routine part of your day. Build up gradually.","Movement contributes to this index. The 8,000-step reference is a model setting, not a requirement for everyone.")
        "Sleep duration" -> FitnessSuggestion(factor.name,"Protect your sleep window","Choose a consistent bedtime and wake time, and leave enough time for rest. Start with one repeatable change tonight.","Recorded sleep duration contributes to the index. Its fixed eight-hour reference is not a personalised sleep prescription.")
        "Recovery" -> FitnessSuggestion(factor.name,"Leave room for recovery","Keep the next session comfortable if you feel tired. Prioritise regular rest and review your sleep rather than chasing a higher strain score.","Recovery is estimated from sleep and overnight HR. A score alone cannot tell you whether you are ready to train.")
        else -> if(lowRecovery) FitnessSuggestion(factor.name,"Build consistency after rest","Keep activity comfortable while recovery is low. Add duration gradually when you feel ready; do not chase a heart-rate zone to lower this number.","Cardio here uses recorded HR-reserve time. Missing HR can lower this component without meaning you exercised less.") else FitnessSuggestion(factor.name,"Choose cardio you enjoy","Plan a manageable walk, ride or other activity you enjoy. Start at a comfortable level and increase gradually over time.","Cardio here uses recorded HR-reserve time, not VO₂ max. Its 30-minute reference is a model setting, not an individual training target.")
    }
}
