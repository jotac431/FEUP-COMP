.class public HelloWorld
.super java/lang/Object

.field private anete Ljava/lang/String;
.field private final tabela LMySymbolTable;

.method public <init>()V
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
.end method

.method public static main([Ljava/lang/String;)V
    .limit stack 2
    .limit locals 2

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc "Hello World!"
    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

    ldc "Hello World Again!"
    astore_1
    getstatic io/IO/out Lio/IO;
    aload_1
    invokevirtual io/IO/println(Ljava/lang/String;)V

    return
.end method