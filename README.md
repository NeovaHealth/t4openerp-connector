
# Tactix4 OpenERP Connector

***

A library designed to connect to an OpenERP server and provide access to a commonly
used subset of OpenERP's API. It fulfills our internal needs, but is not a complete
implementation.
Please fork and submit pull requests, or raise issues, concerning missing functionality
or bugs.

## Motivation

Largely influenced by [DeBortoli Wine's OpenERP Java API](https://github.com/DeBortoliWines/openerp-java-api)
we wanted something that would work more effectively in a scala environment as well as make use of scala's
inherent async support through the Futures mechanism.

## Use Case

```scala

  val connector = new OpenERPConnector("http", "localhost",8069)

  val session = connector.startSession(username,password,database)

  val results = for {
    s <- session
    sAr <- s.searchAndRead("res.partner",  ("email" ilike "info@") AND ("is_company" =/= true) )
  } yield sAr

  results.onComplete(_ match {
    case Success(s) => println("Success: " + s)
    case Failure(f) => println("Failure: " + f.getMessage)
  })

```

For further use cases see the unit tests.

## Details

The transport mechanism is currently fulfilled by the t4xmlrpc library. However,
since the xml-rpc interface is likely to be superseded by the json-rpc interface at some
stage, it was decided to try and de-couple the transport and allow easy implementation of
other transport strategies when needed.

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
#### Fields

There is limited provision for the different relational types of fields.

As with the OpenERP Java API we make a special case of many2many relationships when calling 'write'.
Specifically we implement the 'replace' option automatically for ease of use.

Additionally, calls to 'write' on many2one fields are checked for the correct types, however there is no
other provision for checking appropriate arguments for other field types.

## License

All code is released under the [GNU Affero General Public License Version 3](http://www.gnu.org/licenses/agpl-3.0.html)

## Contribute

Please report any bugs/issues or submit a pull request.