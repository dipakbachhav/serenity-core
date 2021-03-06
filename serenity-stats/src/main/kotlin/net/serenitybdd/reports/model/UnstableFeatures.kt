package net.serenitybdd.reports.model

import net.thucydides.core.model.DataTableRow
import net.thucydides.core.model.Story
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestResult.*
import net.thucydides.core.reports.TestOutcomes
import net.thucydides.core.reports.html.ReportNameProvider
import net.thucydides.core.requirements.ParentRequirementProvider
import net.thucydides.core.requirements.model.Requirement
import java.util.*

class UnstableFeatures{

    companion object {
        @JvmStatic fun from(testOutcomes: TestOutcomes) = UnstableFeaturesBuilder(testOutcomes)
    }
}

class UnstableFeaturesBuilder(val testOutcomes: TestOutcomes) {

    var parentNameProvider : ParentRequirementProvider = DummyParentRequirementProvider()

    fun withRequirementsFrom(parentNameProvider : ParentRequirementProvider) : UnstableFeaturesBuilder {
        this.parentNameProvider = parentNameProvider
        return this
    }

    fun withMaxOf(maxEntries: Int): List<UnstableFeature> {
        val failingTestCount = unsuccessfulOutcomesIn(testOutcomes.unsuccessfulTests)
        return testOutcomes.unsuccessfulTests.outcomes
                .groupBy { outcome -> outcome.userStory }
                .map { (userStory, outcomes) ->
                    UnstableFeature(userStory.displayName,
                            failingTestCount,
                            percentageFailures(failingTestCount, userStory, testOutcomes),
                            featureReport(outcomes[0]))
                }
                .sortedByDescending { unstableFeature -> unstableFeature.failurePercentage }
                .take(maxEntries)
    }

    private fun unsuccessfulOutcomesIn(testOutcomes: TestOutcomes) : Int {
        return testOutcomes.outcomes.map { unsuccessfulOutcomesIn(it) }.sum()
    }

    private fun unsuccessfulOutcomesIn(outcome : TestOutcome) : Int {
        if (outcome.isDataDriven) {
            return outcome.dataTable.rows.count(this::isUnsuccessful)
        }
        return if (outcome.isError || outcome.isCompromised || outcome.isFailure) 1 else 0
    }

    private fun isUnsuccessful(row: DataTableRow) = row.result == FAILURE || row.result == ERROR || row.result == COMPROMISED

    private fun percentageFailures(failingScenarios: Int, userStory: Story, testOutcomes: TestOutcomes): Int {
        val totalScenarios = TestOutcomes.of(testOutcomes.outcomes.filter { outcome -> userStory.equals(outcome.userStory)}).total
        return if (totalScenarios == 0) 0 else failingScenarios * 100 / totalScenarios
    }

    fun featureReport(outcome : TestOutcome) : String {

        val parentRequirement = parentNameProvider.getParentRequirementFor(outcome)

        if (!parentRequirement.isPresent) { return "#" }

        return ReportNameProvider().forRequirement(parentRequirement.get())
    }

}

class DummyParentRequirementProvider : ParentRequirementProvider {
    override fun getParentRequirementFor(testOutcome: TestOutcome?): Optional<Requirement> = Optional.empty()
}

class UnstableFeature(val name: String, val failureCount: Int, val failurePercentage: Int, val report: String)
