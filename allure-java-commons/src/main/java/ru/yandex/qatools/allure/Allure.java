package ru.yandex.qatools.allure;

import ru.yandex.qatools.allure.config.AllureConfig;
import ru.yandex.qatools.allure.events.*;
import ru.yandex.qatools.allure.model.Status;
import ru.yandex.qatools.allure.model.Step;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.allure.model.TestSuiteResult;
import ru.yandex.qatools.allure.storages.StepStorage;
import ru.yandex.qatools.allure.storages.TestCaseStorage;
import ru.yandex.qatools.allure.storages.TestSuiteStorage;

import static ru.yandex.qatools.allure.utils.AllureResultsUtils.writeTestSuiteResult;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 11.11.13
 *         <p/>
 *         Allure Java API. Use this class to access to Allure lifecycle
 */
public class Allure {

    public static final Allure LIFECYCLE = new Allure();

    private static final Object TEST_SUITE_ADD_CHILD_LOCK = new Object();

    private final StepStorage stepStorage = new StepStorage();

    private final TestCaseStorage testCaseStorage = new TestCaseStorage();

    private final TestSuiteStorage testSuiteStorage = new TestSuiteStorage();

    /**
     * Package private. Use Allure.LIFECYCLE singleton
     */
    Allure() {
    }

    /**
     * Process StepStartedEvent. New step will be created and added to
     * stepStorage.
     *
     * @param event to process
     */
    public void fire(StepStartedEvent event) {
        Step step = new Step();
        event.process(step);
        stepStorage.put(step);
    }

    /**
     * Process any StepEvent. You can change last added to stepStorage
     * step using this method.
     *
     * @param event to process
     */
    public void fire(StepEvent event) {
        Step step = stepStorage.getLast();
        event.process(step);
    }

    /**
     * Process StepFinishedEvent. Change last added to stepStorage step
     * and add it as child of previous step.
     *
     * @param event to process
     */
    public void fire(StepFinishedEvent event) {
        Step step = stepStorage.pollLast();
        event.process(step);
        stepStorage.getLast().getSteps().add(step);
    }

    /**
     * Process TestCaseStartedEvent. New testCase will be created and added
     * to suite as child.
     *
     * @param event to process
     */
    public void fire(TestCaseStartedEvent event) {
        //init root step in parent thread if needed
        stepStorage.get();

        TestCaseResult testCase = testCaseStorage.get();
        event.process(testCase);

        synchronized (TEST_SUITE_ADD_CHILD_LOCK) {
            testSuiteStorage.get(event.getSuiteUid()).getTestCases().add(testCase);
        }
    }

    /**
     * Process TestCaseEvent. You can change current testCase context
     * using this method.
     *
     * @param event to process
     */
    public void fire(TestCaseEvent event) {
        TestCaseResult testCase = testCaseStorage.get();
        event.process(testCase);
    }

    /**
     * Process TestCaseFinishedEvent. Add steps and attachments from
     * top step from stepStorage to current testCase, then remove testCase
     * and step from stores. Also remove attachments matches removeAttachments
     * config.
     *
     * @param event to process
     */
    public void fire(TestCaseFinishedEvent event) {
        TestCaseResult testCase = testCaseStorage.get();
        event.process(testCase);

        Step root = stepStorage.pollLast();

        if (Status.PASSED.equals(testCase.getStatus())) {
            new RemoveAttachmentsEvent(AllureConfig.newInstance().getRemoveAttachments()).process(root);
        }

        testCase.getSteps().addAll(root.getSteps());
        testCase.getAttachments().addAll(root.getAttachments());

        stepStorage.remove();
        testCaseStorage.remove();
    }

    /**
     * Process TestSuiteEvent. You can use this method to change current
     * testSuite context. Using event.getUid() to access testSuite.
     *
     * @param event to process
     */
    public void fire(TestSuiteEvent event) {
        TestSuiteResult testSuite = testSuiteStorage.get(event.getUid());
        event.process(testSuite);
    }

    /**
     * Process TestSuiteFinishedEvent. Using event.getUid() to access testSuite.
     * Then remove this suite from storage and marshal testSuite to xml using
     * AllureResultsUtils.writeTestSuiteResult()
     *
     * @param event to process
     */
    public void fire(TestSuiteFinishedEvent event) {
        String suiteUid = event.getUid();
        TestSuiteResult testSuite = testSuiteStorage.get(suiteUid);
        event.process(testSuite);
        testSuiteStorage.remove(suiteUid);

        writeTestSuiteResult(testSuite);
    }

    /**
     * This method just clear current step context.
     *
     * @param event will be ignored
     */
    @SuppressWarnings("unused")
    public void fire(ClearStepStorageEvent event) {
        stepStorage.remove();
    }

    /**
     * This method just clear current testCase context.
     *
     * @param event will be ignored
     */
    @SuppressWarnings("unused")
    public void fire(ClearTestStorageEvent event) {
        testCaseStorage.remove();
    }

    /**
     * Package private. For tests only.
     *
     * @return stepStorage
     */
    StepStorage getStepStorage() {
        return stepStorage;
    }

    /**
     * Package private. For tests only.
     *
     * @return testCaseStorage
     */
    TestCaseStorage getTestCaseStorage() {
        return testCaseStorage;
    }

    /**
     * Package private. For tests only.
     *
     * @return testSuiteStorage
     */
    TestSuiteStorage getTestSuiteStorage() {
        return testSuiteStorage;
    }

    /**
     * Use this method to get Allure version in runtime. Supported since
     * version 1.3.6
     *
     * @return current Allure version
     */
    public String getVersion() {
        return AllureConfig.newInstance().getVersion();
    }
}
