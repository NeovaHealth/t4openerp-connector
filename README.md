
# Tactix4 OpenERP Connector
[![Build Status](https://travis-ci.org/Tactix4/t4openerp-connector.svg?branch=master)](https://travis-ci.org/Tactix4/t4openerp-connector)

***

A library designed to connect to an OpenERP server and provide access to a commonly
used subset of OpenERP's API.

## 2.0.1 Release

Using newer 2.0.2 version of [t4xmlrpc](https://github.com/Tactix4/t4xmlrpc). 
Removed various custom data structures favouring the scalaz versions instead.
The execution context is now an implicit parameter, allowing you to provide
your own context.
 
## 2.0 Release

Major rewrite and fundamental change of approach. Ideas shamelessly lifted from the
[Argonaut](https://github.com/argonaut-io/argonaut) Json parsing library. It is now
purely functional, with no throwing of exceptions and on the whole significantly simplified.

## Using

Add the following to your build.sbt:

  ```"com.tactix4" %% "t4openerp-connector" % "2.0.1"```

## Use Case

```scala

    import scala.concurrent.ExecutionContext.Implicits.global

    val session = new OpenERPConnector("http", "localhost",8069).startSession(username,password,database)

    val ids = session.search("res.partner", "name" ilike "peter")

    ids.bimap(
      (error:String)  => logger.error(s"That didn't work: $error" ),
      (ids:List[Int]) => logger.info(s"The ids: $ids"))

  })

```

For further use cases see the unit tests.

## Motivation

Originally influenced by [DeBortoli Wine's OpenERP Java API](https://github.com/DeBortoliWines/openerp-java-api)
we wanted something that would work more effectively in a scala environment as well as make use of scala's
inherent async support through the Futures mechanism.

#### Domains

With the use of the Domain trait, in conjunction with the implicits defined in Domain's
companion object, a DSL for easily and clearly specifying complex domains is provided

```scala
val complexDomain = ("name" === "ABC") AND ("lang" =/= "EN") AND (NOT("department" child_of "HR") OR ("country" like "Germany"))
```
Associativity is to the *LEFT* - so parenthesis should be used to make precedence unambiguous

```scala
// equates to all all Jim's with ID's of one, OR Jills.
val ambig = ("id" === 1) AND ("name" === "jim") OR ("name" == "jill")

// equates to any Jim or Jill with an id of 1
val nonAmbig = ("id" === 1) AND (("name" === "jim") OR ("name" == "jill"))
```

## License

All code is released under the [GNU Affero General Public License Version 3](http://www.gnu.org/licenses/agpl-3.0.html)

## Contribute

Please report any bugs/issues or submit a pull request.
