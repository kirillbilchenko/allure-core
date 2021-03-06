package ru.yandex.qatools.allure.utils;

import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.qatools.allure.events.TestCaseStartedEvent;
import ru.yandex.qatools.allure.events.TestSuiteStartedEvent;
import ru.yandex.qatools.allure.model.Label;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.lang.annotation.Annotation;
import java.util.*;

import static ru.yandex.qatools.allure.config.AllureModelUtils.*;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 13.12.13
 *         <p/>
 *         Util, using to collect information from class and method annotations
 */
public class AnnotationManager {

    private Collection<Annotation> annotations;

    /**
     * Construct AnnotationManager using given annotations
     *
     * @param annotations initial value for annotations
     */
    public AnnotationManager(Collection<Annotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * Construct AnnotationManager using given annotations
     *
     * @param annotations initial value for annotations
     */
    public AnnotationManager(Annotation... annotations) {
        this.annotations = Arrays.asList(annotations);
    }

    /**
     * Sets into specified {@link ru.yandex.qatools.allure.events.TestSuiteStartedEvent}
     * information from Allure annotations.
     *
     * @param event to change
     */
    public void update(TestSuiteStartedEvent event) {
        if (isTitleAnnotationPresent()) {
            event.setTitle(getTitle());
        }

        if (isDescriptionAnnotationPresent()) {
            event.setDescription(getDescription());
        }

        event.getLabels().addAll(getStoryLabels());
        event.getLabels().addAll(getFeatureLabels());
    }

    /**
     * Sets into specified {@link ru.yandex.qatools.allure.events.TestCaseStartedEvent}
     * information from Allure annotations.
     *
     * @param event to change
     */
    public void update(TestCaseStartedEvent event) {
        if (isTitleAnnotationPresent()) {
            event.setTitle(getTitle());
        }

        if (isDescriptionAnnotationPresent()) {
            event.setDescription(getDescription());
        }

        if (isSeverityAnnotationPresent()) {
            event.getLabels().add(createSeverityLabel(getSeverity()));
        }

        event.getLabels().addAll(getStoryLabels());
        event.getLabels().addAll(getFeatureLabels());
    }

    /**
     * @return true if {@link ru.yandex.qatools.allure.annotations.Title}
     * annotation present in {@link #annotations} and false otherwise
     */
    public boolean isTitleAnnotationPresent() {
        return isAnnotationPresent(Title.class);
    }

    /**
     * @return true if {@link ru.yandex.qatools.allure.annotations.Description}
     * annotation present in {@link #annotations} and false otherwise
     */
    public boolean isDescriptionAnnotationPresent() {
        return isAnnotationPresent(Description.class);
    }

    /**
     * @return true if {@link ru.yandex.qatools.allure.annotations.Severity}
     * annotation present in {@link #annotations} and false otherwise
     */
    public boolean isSeverityAnnotationPresent() {
        return isAnnotationPresent(Severity.class);
    }

    /**
     * @return true if {@link ru.yandex.qatools.allure.annotations.Stories}
     * annotation present in {@link #annotations} and false otherwise
     */
    public boolean isStoriesAnnotationPresent() {
        return isAnnotationPresent(Stories.class);
    }

    /**
     * @return true if {@link ru.yandex.qatools.allure.annotations.Features}
     * annotation present in {@link #annotations} and false otherwise
     */
    public boolean isFeaturesAnnotationPresent() {
        return isAnnotationPresent(Features.class);
    }

    /**
     * Find first {@link ru.yandex.qatools.allure.annotations.Title} annotation
     *
     * @return title or null if annotation doesn't present
     */
    public String getTitle() {
        Title title = getAnnotation(Title.class);
        return title == null ? null : title.value();
    }

    /**
     * Find first {@link ru.yandex.qatools.allure.annotations.Description} annotation
     *
     * @return {@link ru.yandex.qatools.allure.model.Description} or null if
     * annotation doesn't present
     */
    public ru.yandex.qatools.allure.model.Description getDescription() {
        Description description = getAnnotation(Description.class);
        return description == null ? null : new ru.yandex.qatools.allure.model.Description()
                .withValue(description.value())
                .withType(description.type());
    }

    /**
     * Find first {@link ru.yandex.qatools.allure.annotations.Severity} annotation
     *
     * @return {@link ru.yandex.qatools.allure.model.SeverityLevel} or null if
     * annotation doesn't present
     */
    public SeverityLevel getSeverity() {
        Severity severity = getAnnotation(Severity.class);
        return severity == null ? null : severity.value();
    }

    /**
     * Construct label for all {@link ru.yandex.qatools.allure.annotations.Stories} annotations
     * using {@link ru.yandex.qatools.allure.config.AllureModelUtils#createStoryLabel(String)}
     *
     * @return {@link java.util.List} of created labels
     */
    public List<Label> getStoryLabels() {
        if (!isAnnotationPresent(Stories.class)) {
            return Collections.emptyList();
        }

        List<Label> result = new ArrayList<>();
        for (String story : getAnnotation(Stories.class).value()) {
            result.add(createStoryLabel(story));
        }
        return result;
    }

    /**
     * Construct label for all {@link ru.yandex.qatools.allure.annotations.Features} annotations
     * using {@link ru.yandex.qatools.allure.config.AllureModelUtils#createFeatureLabel(String)}
     *
     * @return {@link java.util.List} of created labels
     */
    public List<Label> getFeatureLabels() {
        if (!isAnnotationPresent(Features.class)) {
            return Collections.emptyList();
        }

        List<Label> result = new ArrayList<>();
        for (String feature : getAnnotation(Features.class).value()) {
            result.add(createFeatureLabel(feature));
        }
        return result;
    }

    /**
     * Returns true if {@link #annotations} contains annotation with specified type,
     * false otherwise
     *
     * @param annotationType annotation type to find
     * @return true if {@link #annotations} contains annotation with specified type,
     * false otherwise
     */
    public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
        for (Annotation each : annotations) {
            if (each.annotationType().equals(annotationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find annotation with specified type into {@link #annotations}
     *
     * @param annotationType annotation type to find
     * @return the first found annotation with specified type, of null if
     * {@link #annotations} doesn't contains such
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation each : annotations) {
            if (each.annotationType().equals(annotationType)) {
                return annotationType.cast(each);
            }
        }
        return null;
    }


}
