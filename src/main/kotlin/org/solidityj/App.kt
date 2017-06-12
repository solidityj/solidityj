package org.solidityj

fun main(args: Array<String>) {
    val input  = """
        contract StandardToken is ERC20 {

          function transfer(address _to, uint _value) {
            msg[_to] = 2;
          }
        }
    """

    println(parseCode(input))
}