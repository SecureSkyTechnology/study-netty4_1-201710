# study-netty4_1-201710
2017年9月～10月にかけて、Netty 4.1 を勉強したときのサンプルコードや調査メモ

* 解説などは本リポジトリのWikiページを参照。

## requirement

* Java8

## 開発環境

* JDK >= 1.8.0_92
* Eclipse >= 4.5.2 (Mars.2 Release), "Eclipse IDE for Java EE Developers" パッケージを使用
* Maven >= 3.3.9 (maven-wrapperにて自動的にDLしてくれる)
* ソースコードやテキストファイル全般の文字コードはUTF-8を使用

## ビルドと実行

* ビルド
```
cd study-netty4_1-201710/
mvnw clean package
```
* 実行(jarファイルから)
```
java -jar (...)/tcpecho-plainjava-(version).jar
java -jar (...)/tcpecho-netty-(version).jar
```
* 実行(Mavenプロジェクトから直接実行)
```
cd study-netty4_1-201710/tcpecho-plainjava/ && mvnw exec:java
cd study-netty4_1-201710/tcpecho-netty/     && mvnw exec:java
```

* 実行すると、どのモードで起動するかメニューが表示される。
  * 実行したいモードと引数を入力してENTER.
  * クライアント系なら引数は接続先ホスト名(IPアドレス)とポート番号
  * サーバ系なら引数は接続受付のポート番号(tcpecho-netty ではsleepミリ秒を追加で指定するモードあり)
* 終了するには基本的にはCtrl-C
  * クライアント系なら Ctrl-D で入力終了することも可能。
* モードをそのままjar実行時の引数としても指定可能。
  * 例 : `java -jar tcpecho-plain-java-(version).jar client localhost 8080`

## Eclipseプロジェクト用の設定

https://github.com/SecureSkyTechnology/howto-eclipse-setup の `setup-type1` を使用。README.mdで以下を参照のこと:

* Ecipseのインストール
* Clean Up/Formatter 設定
* GitでcloneしたMavenプロジェクトのインポート 

