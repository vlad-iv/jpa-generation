package com.github.vladiv.jpageneration.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8


/**
 * JPA bean generator.
 * Generate repository, service, controller, facade for entity.
 *
 * @author Vladimir Ivanov (Vladimir.Ivanov@cma.ru)
 */
class JpaAction : AnAction() {
    val dictionaryRepository = "package se.highex.udmp.dao;\n" +
            "\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "\n" +
            "public interface %NAME%Repository extends DictionaryRepository<%NAME%> {\n" +
            "}\n"
    val modifiedRepository = "package se.highex.udmp.dao;\n" +
            "\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "\n" +
            "public interface %NAME%Repository extends ModifiedRepository<%NAME%> {\n" +
            "}\n"
    val dictionaryService = "package se.highex.udmp.service;\n" +
            "\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "\n" +
            "import se.highex.udmp.dao.DictionaryRepository;\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "\n" +
            "@Service\n" +
            "public class %NAME%Service extends DictionaryService<%NAME%> {\n" +
            "\n" +
            "\t@Autowired\n" +
            "\tpublic %NAME%Service(DictionaryRepository<%NAME%> repository) {\n" +
            "\t\tsuper(repository);\n" +
            "\t}\n" +
            "}\n"
    val modifiedService = "package se.highex.udmp.service;\n" +
            "\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "\n" +
            "import se.highex.udmp.dao.ModifiedRepository;\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "\n" +
            "@Service\n" +
            "public class %NAME%Service extends ModifiedService<%NAME%, ModifiedRepository<%NAME%>> {\n" +
            "\n" +
            "\t@Autowired\n" +
            "\tpublic %NAME%Service(ModifiedRepository<%NAME%> repository) {\n" +
            "\t\tsuper(repository);\n" +
            "\t}\n" +
            "}\n"
    val dictionaryController = "package se.highex.udmp.controller;\n" +
            "\n" +
            "import org.springframework.web.bind.annotation.RequestMapping;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n" +
            "\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "import se.highex.udmp.service.%NAME%Service;\n" +
            "\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/%PATH%\")\n" +
            "public class %NAME%Controller extends DictionaryController<%NAME%> {\n" +
            "\t%NAME%Controller(%NAME%Service service) {\n" +
            "\t\tsuper(service);\n" +
            "\t}\n" +
            "\n" +
            "}\n"
    val modifiedController = "package se.highex.udmp.controller;\n" +
            "\n" +
            "import org.springframework.data.domain.Page;\n" +
            "import org.springframework.data.domain.Pageable;\n" +
            "import org.springframework.data.jpa.domain.Specification;\n" +
            "import org.springframework.web.bind.annotation.GetMapping;\n" +
            "import org.springframework.web.bind.annotation.RequestMapping;\n" +
            "import org.springframework.web.bind.annotation.RequestParam;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n" +
            "\n" +
            "import se.highex.udmp.dto.PageResponse;\n" +
            "import se.highex.udmp.filter.NameSpecification;\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "import se.highex.udmp.service.%NAME%Service;\n" +
            "\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/%PATH%\")\n" +
            "public class %NAME%Controller extends BaseController<%NAME%, %NAME%Service> {\n" +
            "\t%NAME%Controller(%NAME%Service service) {\n" +
            "\t\tsuper(service);\n" +
            "\t}\n" +
            "\n" +
            "\t@GetMapping(\"/search\")\n" +
            "\tPageResponse<Page<%NAME%>> search(@RequestParam String name, Pageable pageable) {\n" +
            "\t\tSpecification<%NAME%> spec = new NameSpecification(name);\n" +
            "\t\treturn new PageResponse<>(service.findAll(spec, pageable));\n" +
            "\t}\n" +
            "}\n"

    val dictionaryFacade = "package se.highex.udmp.client;\n" +
            "\n" +
            "import org.springframework.stereotype.Service;\n" +
            "\n" +
            "import se.highex.udmp.model.%NAME%;\n" +
            "\n" +
            "@Service\n" +
            "public class %NAME%Facade extends RestFacade<%NAME%> {\n" +
            "\n" +
            "\tpublic %NAME%Facade() {\n" +
            "\t\tsuper(%NAME%.class);\n" +
            "\t}\n" +
            "\n" +
            "\t@Override\n" +
            "\tprotected String getPath() {\n" +
            "\t\treturn \"%PATH%\";\n" +
            "\t}\n" +
            "\n" +
            "}\n"

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.getData(PlatformDataKeys.PROJECT)


        val title = "JPA Bean Generation"

//        val tree = e.getData(FileSystemTree.DATA_KEY)
//        if (tree == null) {
//            Messages.showMessageDialog(project, "FileSystemTree is empty", title, Messages.getInformationIcon())
//            return
//        }

        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        val sb = java.lang.StringBuilder("Entities:\n")

        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    // TODO
                    continue
                }
                val name = file.nameWithoutExtension
                val path = file.nameWithoutExtension + "s"
                val fileContent = String(file.contentsToByteArray())
                val dictionary = fileContent.contains("extends DictionaryEntity")
                val modified = fileContent.contains("extends MActivityEntity") || fileContent.contains("extends ModifiedEntity")
                if (!dictionary && !modified) {
                    continue
                }
                val type: String = when {
                    dictionary -> "dictionary"
                    else -> "modified"
                }
                sb.append("Found: ").append(name).append(", type: ").append(type).append("\n")
                // Create Repository
                var content = modifiedRepository
                if (dictionary) {
                    content = dictionaryRepository
                }
                val repoFileName = name + "Repository"
                var dao = file.parent.findFileByRelativePath("../dao")
                if (dao == null) {
                    dao = file.parent.parent.createChildDirectory(null, "dao")
                }
                try {
                    var repoFile = dao.createChildData(null, "$repoFileName.java")
                    repoFile.setBinaryContent(content.replace("%NAME%", name).toByteArray(UTF_8))
//                    val outputStream = repoFile.getOutputStream(null)
//                    outputStream.bufferedWriter(UTF_8).use { out ->
//                        out.write()
//                    }
                    sb.append("Created: ").append(repoFileName).append("\n")
                } catch (e: IOException) {
                    sb.append("Exist: ").append(repoFileName).append("\n")
//                    Messages.showMessageDialog(
//                        UIBundle.message("create.new.file.could.not.create.file.error.message", repoFileName),
//                        UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
//                    )
                }
                // Create Service
                val serviceFileName = name + "Service"

                var service = file.parent.findFileByRelativePath("../../../../../../../../core/src/main/java/se/highex/udmp/service")
                if (service == null) {
                    service = file.parent.findFileByRelativePath("../service")
                    if (service == null) {
                        service = file.parent.parent.createChildDirectory(null, "service")
                    }
                }
                if (dictionary) {
                    content = dictionaryService
                } else {
                    content = modifiedService
                }
                try {
                    var serviceFile = service.createChildData(null, "$serviceFileName.java")
                    serviceFile.setBinaryContent(content.replace("%NAME%", name).toByteArray(UTF_8))
                    sb.append("Created: ").append(serviceFileName).append("\n")
                } catch (e: IOException) {
                    sb.append("Exist: ").append(serviceFileName).append("\n")
//                    Messages.showMessageDialog(
//                        UIBundle.message("create.new.file.could.not.create.file.error.message", serviceFileName),
//                        UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
//                    )
                }
                // Create Controller
                val controllerFileName = name + "Controller"

                var controller = file.parent.findFileByRelativePath("../../../../../../../../api/src/main/java/se/highex/udmp/controller")
                if (controller == null) {
                    controller = file.parent.findFileByRelativePath("../controller")
                    if (controller == null) {
                        controller = file.parent.parent.createChildDirectory(null, "controller")
                    }
                }
                if (dictionary) {
                    content = dictionaryController
                } else {
                    content = modifiedController
                }
                try {
                    var controllerFile = controller.createChildData(null, "$controllerFileName.java")
                    controllerFile.setBinaryContent(
                        content
                            .replace("%NAME%", name)
                            .replace("%PATH%", path)
                            .toByteArray(UTF_8)
                    )
                    sb.append("Created: ").append(controllerFileName).append("\n")
                } catch (e: IOException) {
                    sb.append("Exist: ").append(controllerFileName).append("\n")
//                    Messages.showMessageDialog(
//                        UIBundle.message("create.new.file.could.not.create.file.error.message", controllerFileName),
//                        UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
//                    )
                }
                // Create client facade
                val facadeFileName = name + "Facade"
                var facade = file.parent.findFileByRelativePath("../../../../../../../../web-ui/src/main/java/se/highex/udmp/client")
                if (facade == null) {
                    facade = file.parent.findFileByRelativePath("../client")
                    if (facade == null) {
                        facade = file.parent.parent.createChildDirectory(null, "client")
                    }
                }
                content = dictionaryFacade
                try {
                    var controllerFile = facade.createChildData(null, "$facadeFileName.java")
                    controllerFile.setBinaryContent(
                        content
                            .replace("%NAME%", name)
                            .replace("%PATH%", path)
                            .toByteArray(UTF_8)
                    )
                    sb.append("Created: ").append(facadeFileName).append("\n")
                } catch (e: IOException) {
                    sb.append("Exist: ").append(facadeFileName).append("\n")
//                    Messages.showMessageDialog(
//                        UIBundle.message("create.new.file.could.not.create.file.error.message", facadeFileName),
//                        UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
//                    )
                }
            }
        }

        // Получаем файл класса текущей операции
//        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // Получаем путь к текущему файлу класса
//        val classPath = psiFile!!.virtualFile.path

        // Отображение диалога
        Messages.showMessageDialog(project, sb.toString(), title, Messages.getInformationIcon())
    }
}