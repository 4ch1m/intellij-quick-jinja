package de.achimonline.quickjinja.python

import de.achimonline.quickjinja.helper.QuickJinjaHelper.Companion.createTempFile
import de.achimonline.quickjinja.python.QuickJinjaPython.Companion.createScriptFile
import de.achimonline.quickjinja.settings.QuickJinjaAppSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class QuickJinjaPythonTest {
    private lateinit var templateFile: File
    private lateinit var dataFile: File

    @BeforeEach
    fun setUp() {
        templateFile = createTempFile().toFile()
        dataFile = createTempFile().toFile()
    }

    @Test
    fun createScriptFile() {
        assertEquals("""
            import sys
            import json
            import jinja2
            import importlib.util
            
            def import_from_path(module_name, file_path):
                spec = importlib.util.spec_from_file_location(module_name, file_path)
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)
                return module
            
            options = {
            }
            
            environment = jinja2.Environment(
                loader=jinja2.FileSystemLoader([], encoding='utf-8', followlinks=True),
                **options
            )
            
            try:
                import ansible.plugins.filter.core
                import ansible.plugins.test.core
            
                environment.filters.update(ansible.plugins.filter.core.FilterModule().filters())
                environment.tests.update(ansible.plugins.test.core.TestModule().tests())
            except:
                ...
            
            with open(r'${templateFile.path}') as template_file:
                template = template_file.read()

            data = {}
            
            try:
                print(environment.from_string(template).render(**data))
            except Exception as exception:
                print(exception)
                exit(1)
        """.trimIndent(), createScriptFile(templateFile).readText())
    }

    @Test
    fun createScriptFile_withOptionalArgs() {
        assertEquals("""
            import sys
            import json
            import jinja2
            import importlib.util
            
            def import_from_path(module_name, file_path):
                spec = importlib.util.spec_from_file_location(module_name, file_path)
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)
                return module
            
            options = {
                'block_start_string': '{%',
                'block_end_string': '%}',
                'variable_start_string': '{{',
                'variable_end_string': '}}',
                'comment_start_string': '{#',
                'comment_end_string': '#}',
                'newline_sequence': '\n',
                'trim_blocks': False,
                'lstrip_blocks': False,
                'keep_trailing_newline': False,
                'autoescape': False
            }
            
            environment = jinja2.Environment(
                loader=jinja2.FileSystemLoader([r'/project/dir', r'/template/dir'], encoding='utf-8', followlinks=True),
                **options
            )
            
            try:
                import ansible.plugins.filter.core
                import ansible.plugins.test.core
            
                environment.filters.update(ansible.plugins.filter.core.FilterModule().filters())
                environment.tests.update(ansible.plugins.test.core.TestModule().tests())
            except:
                ...
            
            with open(r'${templateFile.path}') as template_file:
                template = template_file.read()
            
            with open(r'${dataFile}') as data_file:
                data = json.load(data_file)
            
            try:
                print(environment.from_string(template).render(**data))
            except Exception as exception:
                print(exception)
                exit(1)
        """.trimIndent(), createScriptFile(
            templateFile,
            dataFile,
            customFiltersFile = null,
            customTestsFile = null,
            QuickJinjaAppSettings().getJinjaOptions(),
            listOf("/project/dir", "/template/dir")
        ).readText())
    }

    @Test
    fun createScriptFile_withCustomFiltersFile() {
        val filtersFile = kotlin.io.path.createTempFile().toFile()
        val testsFile = kotlin.io.path.createTempFile().toFile()

        assertEquals("""
            import sys
            import json
            import jinja2
            import importlib.util
            
            def import_from_path(module_name, file_path):
                spec = importlib.util.spec_from_file_location(module_name, file_path)
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)
                return module
            
            options = {
            }
            
            environment = jinja2.Environment(
                loader=jinja2.FileSystemLoader([], encoding='utf-8', followlinks=True),
                **options
            )
            
            try:
                import ansible.plugins.filter.core
                import ansible.plugins.test.core
            
                environment.filters.update(ansible.plugins.filter.core.FilterModule().filters())
                environment.tests.update(ansible.plugins.test.core.TestModule().tests())
            except:
                ...

            try:
                environment.filters.update(import_from_path('quick_jinja_custom_filters', '${filtersFile.absolutePath}').filters())
            except:
                ...
            
            try:
                environment.tests.update(import_from_path('quick_jinja_custom_tests', '${testsFile.absolutePath}').tests())
            except:
                ...
            
            with open(r'${templateFile.path}') as template_file:
                template = template_file.read()
            
            data = {}
            
            try:
                print(environment.from_string(template).render(**data))
            except Exception as exception:
                print(exception)
                exit(1)
        """.trimIndent(), createScriptFile(
            templateFile = templateFile,
            customFiltersFile = filtersFile,
            customTestsFile = testsFile
        ).readText())
    }

    @AfterEach
    fun tearDown() {
        templateFile.delete()
        dataFile.delete()
    }
}
