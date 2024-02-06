package com.github.nikolaikopernik.codecomplexity.core

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import io.ktor.utils.io.core.*
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import java.io.File
import java.security.MessageDigest
import java.util.*
import kotlin.text.toByteArray

/**
 * This inspection will go other the files and parse the methods only
 * The complexity for those methods will be calculated and put into the specified filder together with the source code
 */
class HighCodeComplexityInspection : LocalInspectionTool() {

    private val targetCodeDirectory = File("/opt/workspace/code-complexity-survey/data").toPath()
    private var database: Database? = null
    /**
     * Check file fast in 2 steps:
     *  - traverse all [PsiElement]s in breadth first order
     *  - once any methods are found - calculate complexity for them
     */
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        if (file.language.isSupportedByComplexityPlugin()) {
            val provider = file.findProviderForElement()
            file.visitFileFast(provider) { complexitySink, element ->
                val text = element.text
                database?.insert(Examples) {
                    set(it.id, text.toSHA256())
                    set(it.lang, file.language.id.lowercase())
                    set(it.complexity, complexitySink.getComplexity())
                    set(it.body, text.toByteArray())
                }
            }
            problems.add(manager.createProblemDescriptor(file,
                                                         "file ${file.name} has been processed",
                                                         isOnTheFly,
                                                         emptyArray(),
                                                         ProblemHighlightType.GENERIC_ERROR_OR_WARNING))
        }
        return problems.toTypedArray()
    }


    override fun inspectionStarted(session: LocalInspectionToolSession, isOnTheFly: Boolean) {
        database = Database.connect("jdbc:sqlite:/opt/workspace/code-complexity-survey/data/examples.db",
                                    driver = "org.sqlite.JDBC")

        database?.useConnection { conn ->
            val sql = "create table if not exists examples(id TEXT, lang TEXT, complexity INTEGER, body BLOB, PRIMARY KEY(id));"
            conn.prepareStatement(sql).executeUpdate()
        }
    }

    override fun inspectionFinished(session: LocalInspectionToolSession, problemsHolder: ProblemsHolder) {

    }
}

val md = MessageDigest.getInstance("SHA-256")
fun String.toSHA256(): String {
    val input = this.toByteArray()
    val bytes = md.digest(input)
    return Base64.getEncoder().encodeToString(bytes)
}
