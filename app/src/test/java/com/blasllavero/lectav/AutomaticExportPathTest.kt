package com.blasllavero.lectav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticExportPathTest {
    @Test fun courseUnitAndSessionDirectoriesAreStable() {
        val course=SavedCourse(
            id="teacher|ES-CT|eso|eso3|matemat",role="teacher",
            pathway="eso",course="eso3",subject="Matemàtiques",
            lastOpenedAt=0L
        )
        val unit=TopicPlan(
            id="t1-u1-a1b2c3d4",title="Fractions",sessions=3,
            source="teacher",trimester=1
        )
        val path=AutomaticExportFolder.relativePath(course,unit,0,2)
        assertEquals("ESO",path[0])
        assertEquals("eso3",path[1])
        assertEquals("Matemàtiques",path[2])
        assertEquals("Trimester_1",path[3])
        assertTrue(path[4].startsWith("Unit_01_"))
        assertEquals("Session_02",path[5])
    }

    @Test fun maliciousPathSymbolsAreSanitized() {
        val course=SavedCourse("id","teacher","eso","eso3",
            "../Maths: Unit / A",0L)
        val unit=TopicPlan(id="../../escape",title="Title",
            sessions=1,source="teacher",trimester=1)
        val path=AutomaticExportFolder.relativePath(course,unit,0,1)
        assertTrue(path.all { ".." !in it && '/' !in it && '\\' !in it })
        assertEquals(6,path.size)
    }
}
