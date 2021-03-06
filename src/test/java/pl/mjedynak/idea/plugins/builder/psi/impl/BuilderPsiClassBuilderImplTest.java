package pl.mjedynak.idea.plugins.builder.psi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import pl.mjedynak.idea.plugins.builder.psi.BuilderPsiClassBuilder;
import pl.mjedynak.idea.plugins.builder.psi.PsiHelper;
import pl.mjedynak.idea.plugins.builder.psi.model.PsiFieldsForBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class BuilderPsiClassBuilderImplTest {

    @InjectMocks private BuilderPsiClassBuilderImpl psiClassBuilder;
    @Mock private PsiHelper psiHelper;
    @Mock private Project project;
    @Mock private PsiDirectory targetDirectory;
    @Mock private PsiClass srcClass;
    @Mock private JavaDirectoryService javaDirectoryService;
    @Mock private PsiClass builderClass;
    @Mock private JavaPsiFacade javaPsiFacade;
    @Mock private PsiElementFactory elementFactory;
    @Mock private PsiFieldsForBuilder psiFieldsForBuilder;
    @Mock private PsiField srcClassNameField;

    private List<PsiField> psiFieldsForSetters;

    private List<PsiField> psiFieldsForConstructor;

    private String builderClassName = "BuilderClassName";
    private String methodPrefix = "with";
    private String srcClassName = "ClassName";
    private String srcClassFieldName = "className";

    @Before
    public void setUp() {
        psiFieldsForConstructor = new ArrayList<PsiField>();
        psiFieldsForSetters = new ArrayList<PsiField>();
        given(psiHelper.getJavaDirectoryService()).willReturn(javaDirectoryService);
        given(javaDirectoryService.createClass(targetDirectory, builderClassName)).willReturn(builderClass);
        given(psiHelper.getJavaPsiFacade(project)).willReturn(javaPsiFacade);
        given(javaPsiFacade.getElementFactory()).willReturn(elementFactory);
        given(srcClass.getName()).willReturn(srcClassName);
        given(psiFieldsForBuilder.getFieldsForConstructor()).willReturn(psiFieldsForConstructor);
        given(psiFieldsForBuilder.getFieldsForSetters()).willReturn(psiFieldsForSetters);
    }

    @SuppressWarnings(value = "unchecked")
    @Test
    public void shouldSetPassedFieldsAndCreateRequiredOnes() {
        // when
        BuilderPsiClassBuilder result = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder);

        // then
        assertThat((BuilderPsiClassBuilderImpl) result, is(psiClassBuilder));
        assertThat((Project) ReflectionTestUtils.getField(psiClassBuilder, "project"), is(project));
        assertThat((PsiDirectory) ReflectionTestUtils.getField(psiClassBuilder, "targetDirectory"), is(targetDirectory));
        assertThat((PsiClass) ReflectionTestUtils.getField(psiClassBuilder, "srcClass"), is(srcClass));
        assertThat((String) ReflectionTestUtils.getField(psiClassBuilder, "builderClassName"), is(builderClassName));
        assertThat((List<PsiField>) ReflectionTestUtils.getField(psiClassBuilder, "psiFieldsForSetters"), is(psiFieldsForSetters));
        assertThat((List<PsiField>) ReflectionTestUtils.getField(psiClassBuilder, "psiFieldsForConstructor"), is(psiFieldsForConstructor));
        assertThat((PsiClass) ReflectionTestUtils.getField(psiClassBuilder, "builderClass"), is(builderClass));
        assertThat((PsiElementFactory) ReflectionTestUtils.getField(psiClassBuilder, "elementFactory"), is(elementFactory));
        assertThat((String) ReflectionTestUtils.getField(psiClassBuilder, "srcClassName"), is(srcClassName));
        assertThat((String) ReflectionTestUtils.getField(psiClassBuilder, "srcClassFieldName"), is(srcClassFieldName));
    }

    @Test
    public void shouldAddFieldsOfCopyToBuilderClassWithoutAnnotationAndFinalModifierAndComments() {
        // given
        String finalModifier = "final";
        PsiField psiFieldForSetters = mock(PsiField.class);
        psiFieldsForSetters.add(psiFieldForSetters);
        PsiField copyPsiFieldForSetter = mock(PsiField.class);
        PsiModifierList psiModifierListForSetter = mock(PsiModifierList.class);
        PsiAnnotation annotation = mock(PsiAnnotation.class);
        given(psiFieldForSetters.copy()).willReturn(copyPsiFieldForSetter);
        given(copyPsiFieldForSetter.getModifierList()).willReturn(psiModifierListForSetter);
        PsiAnnotation[] annotationArray = createAnnotationArray(annotation);
        given(psiModifierListForSetter.getAnnotations()).willReturn(annotationArray);

        PsiField psiFieldForConstructor = mock(PsiField.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        PsiField copyPsiFieldForConstructor = mock(PsiField.class);
        PsiModifierList psiModifierListForConstructor = mock(PsiModifierList.class, RETURNS_MOCKS);
        given(psiModifierListForConstructor.hasExplicitModifier(finalModifier)).willReturn(true);
        given(psiFieldForConstructor.copy()).willReturn(copyPsiFieldForConstructor);
        given(copyPsiFieldForConstructor.getModifierList()).willReturn(psiModifierListForConstructor);
        PsiDocComment docComment = mock(PsiDocComment.class);
        given(copyPsiFieldForConstructor.getDocComment()).willReturn(docComment);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withFields();

        // then
        verify(annotation).delete();
        verify(psiModifierListForConstructor).setModifierProperty(finalModifier, false);
        verify(docComment).delete();
        verify(builderClass).add(copyPsiFieldForSetter);
        verify(builderClass).add(copyPsiFieldForConstructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddPrivateConstructorToBuildClass() {
        // given
        PsiMethod constructor = mock(PsiMethod.class);
        PsiModifierList modifierList = mock(PsiModifierList.class);
        given(constructor.getModifierList()).willReturn(modifierList);
        given(elementFactory.createConstructor()).willReturn(constructor);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withPrivateConstructor();

        // then
        verify(modifierList).setModifierProperty("private", true);
        verify(builderClass).add(constructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddInitializingMethod() {
        // given
        PsiMethod method = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText(
                "public static " + builderClassName + " a" + srcClassName + "() { return new " + builderClassName + "();}", srcClass)).willReturn(method);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withInitializingMethod();

        // then
        verify(builderClass).add(method);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddInitializingMethodStartingWithAnIfSourceClassNameStartsWithVowel() {
        // given
        PsiMethod method = mock(PsiMethod.class);
        String srcClassNameStartingWithVowel = "Inventory";
        given(srcClass.getName()).willReturn(srcClassNameStartingWithVowel);
        given(elementFactory.createMethodFromText(
                "public static " + builderClassName + " an" + srcClassNameStartingWithVowel + "() { return new " + builderClassName + "();}", srcClass)).willReturn(method);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withInitializingMethod();

        // then
        verify(builderClass).add(method);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddSetMethodsForFieldsFromBothLists() {
        // given
        PsiFieldImpl psiFieldForSetter = mock(PsiFieldImpl.class);
        psiFieldsForSetters.add(psiFieldForSetter);
        given(psiFieldForSetter.getName()).willReturn("name");
        PsiType typeForFieldForSetter = mock(PsiType.class);
        given(typeForFieldForSetter.getPresentableText()).willReturn("String");
        given(psiFieldForSetter.getType()).willReturn(typeForFieldForSetter);
        PsiMethod methodForFieldForSetter = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + builderClassName + " withName(String name) { this.name = name; return this; }", psiFieldForSetter))
                .willReturn(methodForFieldForSetter);

        PsiFieldImpl psiFieldForConstructor = mock(PsiFieldImpl.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        given(psiFieldForConstructor.getName()).willReturn("age");
        PsiType typeForFieldForConstructor = mock(PsiType.class);
        given(typeForFieldForConstructor.getPresentableText()).willReturn("int");
        given(psiFieldForConstructor.getType()).willReturn(typeForFieldForConstructor);
        PsiMethod methodForFieldForConstructor = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + builderClassName + " withAge(int age) { this.age = age; return this; }", psiFieldForConstructor))
                .willReturn(methodForFieldForConstructor);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withSetMethods(methodPrefix);

        // then
        verify(builderClass).add(methodForFieldForSetter);
        verify(builderClass).add(methodForFieldForConstructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddSetMethodsWithPrefixForFieldsFromBothLists() {
        // given
        PsiFieldImpl psiFieldForSetter = mock(PsiFieldImpl.class);
        psiFieldsForSetters.add(psiFieldForSetter);
        given(psiFieldForSetter.getName()).willReturn("name");
        PsiType typeForFieldForSetter = mock(PsiType.class);
        given(typeForFieldForSetter.getPresentableText()).willReturn("String");
        given(psiFieldForSetter.getType()).willReturn(typeForFieldForSetter);
        PsiMethod methodForFieldForSetter = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + builderClassName + " name(String name) { this.name = name; return this; }", psiFieldForSetter))
                .willReturn(methodForFieldForSetter);

        PsiFieldImpl psiFieldForConstructor = mock(PsiFieldImpl.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        given(psiFieldForConstructor.getName()).willReturn("age");
        PsiType typeForFieldForConstructor = mock(PsiType.class);
        given(typeForFieldForConstructor.getPresentableText()).willReturn("int");
        given(psiFieldForConstructor.getType()).willReturn(typeForFieldForConstructor);
        PsiMethod methodForFieldForConstructor = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + builderClassName + " age(int age) { this.age = age; return this; }", psiFieldForConstructor))
                .willReturn(methodForFieldForConstructor);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withSetMethods("");

        // then
        verify(builderClass).add(methodForFieldForSetter);
        verify(builderClass).add(methodForFieldForConstructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldReturnBuilderObjectWithBuildMethodUsingSetterAndConstructor() {
        // given
        PsiField psiFieldForSetter = mock(PsiField.class);
        psiFieldsForSetters.add(psiFieldForSetter);

        PsiField psiFieldForConstructor = mock(PsiField.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        given(psiFieldForConstructor.getName()).willReturn("age");

        given(psiFieldForSetter.getName()).willReturn("name");
        PsiMethod method = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + srcClassName + " build() { " + srcClassName + " " + srcClassFieldName + " = new " + srcClassName + "(age);"
                + srcClassFieldName + ".setName(name);return " + srcClassFieldName + ";}", srcClass)).willReturn(method);
        // when
        PsiClass result = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).build();

        // then
        verify(builderClass).add(method);
        verifyNoMoreInteractions(builderClass);
        assertThat(result, is(notNullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithPrivateConstructorIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withPrivateConstructor();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithInitializingMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withInitializingMethod();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithSetMethodsIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withSetMethods("with");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithFieldsMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withFields();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingBuildMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.build();
    }

    private PsiAnnotation[] createAnnotationArray(PsiAnnotation annotation) {
        PsiAnnotation[] annotationArray = new PsiAnnotation[1];
        annotationArray[0] = annotation;
        return annotationArray;
    }
}
