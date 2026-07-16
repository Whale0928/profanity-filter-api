package app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import app.security.annotation.VerifiedClientOnly;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@AnalyzeClasses(packages = "app", importOptions = ImportOption.DoNotIncludeTests.class)
class OpenApiArchitectureTest {

  private static final Logger log = LoggerFactory.getLogger(OpenApiArchitectureTest.class);

  private static final String OPENAPI_PACKAGE = "app.openapi";
  private static final String API_KEY_SECURITY_NAME = "ApiKeyAuth";

  private static final DescribedPredicate<JavaClass> DISALLOWED_SWAGGER_ANNOTATION =
      new DescribedPredicate<>("컨트롤러에서 직접 사용하면 안 되는 Swagger 문서 어노테이션") {
        @Override
        public boolean test(JavaClass javaClass) {
          return DISALLOWED_PRESENTATION_SWAGGER_ANNOTATIONS.contains(javaClass.getName());
        }
      };

  private static final DescribedPredicate<JavaMethod> CONTROLLER_ENDPOINT =
      new DescribedPredicate<>("컨트롤러 엔드포인트 메서드") {
        @Override
        public boolean test(JavaMethod method) {
          return isControllerEndpoint(method);
        }
      };

  private static final DescribedPredicate<JavaMethod> VERIFIED_CLIENT_ENDPOINT =
      new DescribedPredicate<>("@VerifiedClientOnly가 적용된 컨트롤러 엔드포인트") {
        @Override
        public boolean test(JavaMethod method) {
          return isVerifiedClientEndpoint(method);
        }
      };

  private static final DescribedPredicate<JavaMethod> DOCUMENTED_CONTROLLER_ENDPOINT =
      new DescribedPredicate<>("공개 OpenAPI 컨트롤러 엔드포인트") {
        @Override
        public boolean test(JavaMethod method) {
          return isControllerEndpoint(method) && !isHidden(method);
        }
      };

  private static final Set<String> ENDPOINT_MAPPING_ANNOTATIONS =
      Set.of(
          DeleteMapping.class.getName(),
          GetMapping.class.getName(),
          PatchMapping.class.getName(),
          PostMapping.class.getName(),
          PutMapping.class.getName(),
          RequestMapping.class.getName());

  private static final Set<String> DOCUMENT_CONTROLLER_NAMES = Set.of("DocumentController");

  private static final Set<String> DISALLOWED_PRESENTATION_SWAGGER_ANNOTATIONS =
      Set.of(
          Operation.class.getName(),
          io.swagger.v3.oas.annotations.Parameter.class.getName(),
          RequestBody.class.getName(),
          ApiResponse.class.getName(),
          SecurityRequirement.class.getName(),
          Content.class.getName(),
          ExampleObject.class.getName(),
          Schema.class.getName(),
          Tag.class.getName());

  @ArchTest
  @DisplayName("컨트롤러는 Hidden 외 Swagger 문서 어노테이션을 직접 사용하지 않는다")
  static void controller_should_not_depend_on_swagger_operation_annotations(JavaClasses classes) {
    log.info("컨트롤러 계층이 @Hidden 외 Swagger 문서 어노테이션에 직접 의존하지 않는지 검사합니다.");

    noClasses()
        .that()
        .resideInAPackage("app.presentation..")
        .should()
        .dependOnClassesThat(DISALLOWED_SWAGGER_ANNOTATION)
        .as("컨트롤러는 @Hidden 외 Swagger 문서 어노테이션을 직접 사용하지 않는다")
        .because("공개 계약은 app.openapi 합성 어노테이션으로 관리하고 비공개 여부만 @Hidden으로 표시합니다.")
        .check(classes);
  }

  @ArchTest
  @DisplayName("컨트롤러 엔드포인트는 공개 문서 또는 Hidden 중 하나로만 분류한다")
  static void controller_endpoint_should_have_exactly_one_visibility_decision(JavaClasses classes) {
    log.info("컨트롤러 엔드포인트의 공개 문서와 @Hidden 배타 분류를 검사합니다.");

    methods()
        .that(CONTROLLER_ENDPOINT)
        .should(haveExactlyOneVisibilityDecision())
        .as("엔드포인트는 app.openapi 합성 어노테이션 하나 또는 @Hidden 중 하나만 가져야 한다")
        .because("공개 여부 미결정과 공개 문서 및 숨김의 중복 선언을 동시에 막기 위한 규칙입니다.")
        .check(classes);
  }

  @ArchTest
  @DisplayName("컨트롤러 엔드포인트는 같은 이름의 OpenAPI holder만 사용한다")
  static void controller_endpoint_should_use_matching_openapi_holder(JavaClasses classes) {
    log.info("컨트롤러와 OpenAPI holder의 이름 기반 매칭을 검사합니다.");

    methods()
        .that(DOCUMENTED_CONTROLLER_ENDPOINT)
        .should(useMatchingOpenApiHolder())
        .as("예: ProfanityController는 ProfanityOpenApi의 합성 어노테이션만 사용해야 한다")
        .because("컨트롤러와 문서 어노테이션의 소유 경계를 1:1로 유지하기 위한 규칙입니다.")
        .check(classes);
  }

  @ArchTest
  @DisplayName("인증 클라이언트 전용 엔드포인트는 ApiKeyAuth 보안 요구사항을 문서에 명시한다")
  static void verified_client_endpoint_should_declare_api_key_security(JavaClasses classes) {
    log.info("@VerifiedClientOnly 엔드포인트의 ApiKeyAuth 문서화를 검사합니다.");

    methods()
        .that(VERIFIED_CLIENT_ENDPOINT)
        .should(declareApiKeySecurity())
        .as("@VerifiedClientOnly 엔드포인트는 OpenAPI @Operation에 ApiKeyAuth security를 가져야 한다")
        .because("인증이 필요한 API가 /openapi.json에서 인증 없는 API처럼 보이는 문제를 막기 위한 규칙입니다.")
        .check(classes);
  }

  @ArchTest
  @DisplayName("OpenAPI 어노테이션 holder는 런타임 계층에 의존하지 않는다")
  static void openapi_annotations_should_not_depend_on_runtime_layers(JavaClasses classes) {
    log.info("app.openapi 패키지가 presentation/application/storage 런타임 계층에 의존하지 않는지 검사합니다.");

    noClasses()
        .that()
        .resideInAPackage("app.openapi..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("app.presentation..", "app.application..", "app.storage..")
        .as("app.openapi 문서 어노테이션 holder는 런타임 계층에 의존하지 않아야 한다")
        .because("문서 계층이 컨트롤러, 서비스, 저장소 구현을 끌어오면 문서 변경의 영향 범위가 커집니다.")
        .check(classes);
  }

  private static boolean isControllerEndpoint(JavaMethod method) {
    return method.getOwner().getPackageName().startsWith("app.presentation")
        && method.getOwner().getSimpleName().endsWith("Controller")
        && !DOCUMENT_CONTROLLER_NAMES.contains(method.getOwner().getSimpleName())
        && hasAnyAnnotation(method, ENDPOINT_MAPPING_ANNOTATIONS);
  }

  private static boolean isVerifiedClientEndpoint(JavaMethod method) {
    return isControllerEndpoint(method)
        && !isHidden(method)
        && method.isAnnotatedWith(VerifiedClientOnly.class);
  }

  private static boolean isHidden(JavaMethod method) {
    return method.isAnnotatedWith(Hidden.class) || method.getOwner().isAnnotatedWith(Hidden.class);
  }

  private static boolean hasAnyAnnotation(JavaMethod method, Set<String> annotationNames) {
    return method.getAnnotations().stream()
        .map(JavaAnnotation::getRawType)
        .map(JavaClass::getName)
        .anyMatch(annotationNames::contains);
  }

  private static ArchCondition<JavaMethod> haveExactlyOneVisibilityDecision() {
    return new ArchCondition<>("have exactly one OpenAPI visibility decision") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        List<JavaAnnotation<JavaMethod>> annotations = openApiAnnotations(method);
        boolean hidden = isHidden(method);
        boolean valid = hidden ? annotations.isEmpty() : annotations.size() == 1;
        if (!valid) {
          events.add(
              SimpleConditionEvent.violated(
                  method,
                  method.getFullName()
                      + " must be either @Hidden or have exactly one app.openapi annotation"
                      + " (hidden="
                      + hidden
                      + ", annotations="
                      + annotations.size()
                      + ")"));
        }
      }
    };
  }

  private static ArchCondition<JavaMethod> useMatchingOpenApiHolder() {
    return new ArchCondition<>("use matching OpenAPI holder") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        String expectedHolder = expectedOpenApiHolderName(method);
        List<String> actualHolders =
            openApiAnnotations(method).stream()
                .map(annotation -> annotation.getRawType().getName())
                .map(OpenApiArchitectureTest::holderName)
                .toList();

        if (!actualHolders.equals(List.of(expectedHolder))) {
          events.add(
              SimpleConditionEvent.violated(
                  method,
                  method.getFullName()
                      + " must use "
                      + expectedHolder
                      + " but uses "
                      + actualHolders));
        }
      }
    };
  }

  private static ArchCondition<JavaMethod> declareApiKeySecurity() {
    return new ArchCondition<>("declare ApiKeyAuth security requirement") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        boolean declared =
            openApiAnnotations(method).stream()
                .map(annotation -> annotation.getRawType().getName())
                .map(OpenApiArchitectureTest::loadAnnotationType)
                .map(annotationType -> annotationType.getAnnotation(Operation.class))
                .anyMatch(OpenApiArchitectureTest::hasApiKeySecurity);

        if (!declared) {
          events.add(
              SimpleConditionEvent.violated(
                  method, method.getFullName() + " must declare ApiKeyAuth security requirement"));
        }
      }
    };
  }

  private static List<JavaAnnotation<JavaMethod>> openApiAnnotations(JavaMethod method) {
    return method.getAnnotations().stream()
        .filter(annotation -> annotation.getRawType().getPackageName().equals(OPENAPI_PACKAGE))
        .toList();
  }

  private static String expectedOpenApiHolderName(JavaMethod method) {
    String controllerName = method.getOwner().getSimpleName();
    return OPENAPI_PACKAGE + "." + controllerName.replace("Controller", "OpenApi");
  }

  private static String holderName(String annotationTypeName) {
    int nestedAnnotationSeparator = annotationTypeName.indexOf('$');
    if (nestedAnnotationSeparator < 0) {
      return annotationTypeName;
    }
    return annotationTypeName.substring(0, nestedAnnotationSeparator);
  }

  private static Class<? extends Annotation> loadAnnotationType(String annotationTypeName) {
    try {
      return Class.forName(annotationTypeName).asSubclass(Annotation.class);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException("OpenAPI annotation class not found: " + annotationTypeName);
    }
  }

  private static boolean hasApiKeySecurity(Operation operation) {
    if (operation == null) {
      return false;
    }
    for (SecurityRequirement securityRequirement : operation.security()) {
      if (API_KEY_SECURITY_NAME.equals(securityRequirement.name())) {
        return true;
      }
    }
    return false;
  }
}
