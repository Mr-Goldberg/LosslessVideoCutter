package com.goldberg.losslessvideocutter

fun <T : Enum<T>> enumValueOf(classOfEnum: Class<T>, value: Int): T
{
    return classOfEnum.enumConstants!![value]
}

fun <T : Enum<T>> enumValueOf(classOfEnum: Class<T>, value: Int, default: T): T
{
    return try
    {
        classOfEnum.enumConstants!![value]
    }
    catch (ex: ArrayIndexOutOfBoundsException)
    {
        default
    }
}
