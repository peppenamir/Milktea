package jp.panta.misskeyandroidclient.util

import org.junit.Test

class StringIndexTest {

    @Test
    fun substringTest(){
        val text = "0123456789"
        println(text.substring(0, 9))
    }

    @Test
    fun whileText(){
        var counter = 0
        while(counter < 10){
            println(counter)
            counter++
        }
        println("終了後:$counter")
    }
}