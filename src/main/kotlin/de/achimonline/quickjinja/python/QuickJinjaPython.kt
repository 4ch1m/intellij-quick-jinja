package de.achimonline.quickjinja.python

import de.achimonline.quickjinja.helper.QuickJinjaHelper.Companion.createTempFileFromText
import java.io.File

class QuickJinjaPython {
    companion object {
        fun createScriptFile(
            templateFile: File,
            variablesFile: File? = null,
            customFiltersFile: File? = null,
            customTestsFile: File? = null,
            options: Map<String, Any>? = null,
            fileSystemLoaderPaths: List<String> = emptyList()
        ): File {
            var scriptFileContent = """
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
            """.trimIndent()

            if (options != null) {
                val optionList = mutableListOf<String>()

                options.forEach { (key, value) ->
                    if (value is Boolean) {
                        optionList.add("    '${key}': ${if (value) "True" else "False"}")
                    } else if (value is String) {
                        if (value.trim().isNotEmpty()) {
                            optionList.add("    '${key}': '${value}'")
                        }
                    }
                }

                scriptFileContent += "${System.lineSeparator()}${optionList.joinToString(",${ System.lineSeparator() }")}"
            }

            scriptFileContent += """${System.lineSeparator()}
                }
                
                environment = jinja2.Environment(
                    loader=jinja2.FileSystemLoader([${fileSystemLoaderPaths.joinToString(", ") { "r'$it'" }}], encoding='utf-8', followlinks=True),
                    **options
                )
                
                try:
                    import ansible.plugins.filter.core
                    import ansible.plugins.test.core
                
                    environment.filters.update(ansible.plugins.filter.core.FilterModule().filters())
                    environment.tests.update(ansible.plugins.test.core.TestModule().tests())
                except:
                    ...
            """.trimIndent()

            if (customFiltersFile != null && customFiltersFile.exists()) {
                scriptFileContent += """${System.lineSeparator()}

                    try:
                        environment.filters.update(import_from_path('quick_jinja_custom_filters', '${customFiltersFile.absolutePath}').filters())
                    except:
                        ...
                """.trimIndent()
            }

            if (customTestsFile != null && customTestsFile.exists()) {
                scriptFileContent += """${System.lineSeparator()}

                    try:
                        environment.tests.update(import_from_path('quick_jinja_custom_tests', '${customTestsFile.absolutePath}').tests())
                    except:
                        ...
                """.trimIndent()
            }

            scriptFileContent += """${System.lineSeparator()}

                with open(r'${templateFile.absolutePath}') as template_file:
                    template = template_file.read()
            """.trimIndent()

            scriptFileContent += if (variablesFile != null) {
                """${System.lineSeparator()}

                    with open(r'${variablesFile.absolutePath}') as data_file:
                        data = json.load(data_file)
                """.trimIndent()
            } else {
                """${System.lineSeparator()}

                    data = {}
                """.trimIndent()
            }

            scriptFileContent += """${System.lineSeparator()}

                try:
                    print(environment.from_string(template).render(**data))
                except Exception as exception:
                    print(exception)
                    exit(1)
            """.trimIndent()

            return createTempFileFromText(scriptFileContent)
        }
    }
}
