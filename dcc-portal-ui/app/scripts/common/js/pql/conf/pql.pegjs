/**
 * This PEG grammar translates PQL into a JSON tree.
 */

/*
 * Helper functions
 */
{
  var altFuncMapping = {
    "&":    "and",
    "|":    "or",
    "=ge=": "ge",
    ">=":   "ge",
    "=le=": "le",
    "<=":   "le",
    "!=":   "ne",
    "<>":   "ne",
    "=gt=": "gt",
    ">":    "gt",
    "=lt=": "lt",
    "<":    "lt",
    "=":    "eq",
    "==":   "eq"
  };

  var toBasicUnit = function (left, op, right) {
    var result = {op: op};
    result.values = right ? {left: left, right: right} : left;
    return result;
  };

  function toSortUnit (field, direction) {
    field = field || "";
    direction = direction || "+";
    return {direction: direction, field: field};
  }

  function toBinaryUnit (op, field, value) {
    field = field || "";
    value = value || "";
    return {op: op, field: field, value: value};
  }

  function toLimitUnit (first, second) {
    var op = "limit";

    if (second !== undefined) {
      return {op: op, from: first, size: second};
    } else {
      return {op: op, size: first};
    }
  }

  function toFieldValueUnit (op, field, values) {
    return {op: op, field: field, values: values};
  }

  function headJoinsTail (head, tail) {
    return head + tail.join("");
  }
}

/**
* Grammar definition starts here.
*/

start = core

core
  = statementArray
  / orderedStatementArray
  / countStatement

statementArray
  = first:(s: statement COMMA {return s;})* last: statement {return first.concat (last);}
  / first:(s: statement COMMA {return s;})* last: limitFilter {return first.concat (last);}
  / first:(s: statement COMMA {return s;})* last: orderAndLimitFilter {return first.concat(last);}

orderedStatementArray
  = first:(s: statement COMMA {return s;})* last: orderFilter {return first.concat (last);}

countStatement
  = count: COUNT COMMA first:(s: filters COMMA {return s;})* last: filters
    {return toBasicUnit (first.concat (last), "count");}

filters
  = uniFunc
  / binFuncSyntax1
  / binFuncSyntax2
  / inFilter
  / naryFuncSyntax1
  / naryFuncSyntax2
  / not
  / nested

statement
  = filters
  / functions


stringArgArray
  = first:(head: STRING COMMA {return head;})* last: STRING {return first.concat(last);}

idArray
  = first:(head: ID COMMA {return head;})* last: ID {return first.concat(last);}

numberArray
  = first:(head: NUMBER COMMA {return head;})* last: NUMBER {return first.concat(last);}


// functions

COUNT
  = "count" OPAR CPAR

functionNames
  = "select" / "facets"

functions
  = _ o:functionNames OPAR args:(idArray / ASTERISK) CPAR
    {return toBasicUnit ([].concat(args), o);}


// filters
//
nested
  = _ "nested" OPAR field: ID COMMA filters: statementArray CPAR
    {return toFieldValueUnit ("nested", field, filters);}

not
  = _ o:"not" OPAR l: statement CPAR
    {return toBasicUnit ([l], o);}


uniFuncOps
  = "exists" / "missing"

uniFunc
  = _ o: uniFuncOps _ OPAR l: ID CPAR
    {return toBasicUnit ([l], o);}


//
binFuncSyntax1Ops
  = "eq" / "ge" / "le" / "ne" / "gt" / "lt"

binFuncSyntax2Ops
  = "=ge=" / "=le=" / "!=" / "=gt=" / "=lt="

// Not supported by PQL but is supported here on the client side for these friendly versions
// NOTE: ">=", "<=" & "<>" must be before ">" & "<"
binFuncSyntax3Ops
  = "==" / ">=" / "<="  / "<>" / ">" / "<"

// We need this alias to define "=" at the very end, which is needed for others to work.
binFuncAltSyntaxOps
  = binFuncSyntax2Ops
  / binFuncSyntax3Ops
  / "="


binFuncSyntax1
  = _ o: binFuncSyntax1Ops _ OPAR l: ID COMMA r: VALUE CPAR
    {return toFieldValueUnit (o, l, [].concat(r));}

binFuncSyntax2
  = _ l: ID _ o: binFuncAltSyntaxOps _ r: VALUE _
    {return toFieldValueUnit (altFuncMapping [o], l, [].concat(r));}


inFilter
  = _ o:"in" OPAR first: ID COMMA rest: valueArray CPAR
    {return toFieldValueUnit (o, first, rest);}

//
naryFuncSyntax1Ops
  = "and" / "or"

naryFuncSyntax2Ops
  = "&" / "|"

naryFuncSyntax1
  = _ o: naryFuncSyntax1Ops _ OPAR l: statementArray CPAR
    {return toBasicUnit (l, o);}

naryFuncSyntax2
  = _ OPAR l: statement _ o: naryFuncSyntax2Ops _ r: statement CPAR _
    {o = altFuncMapping[o]; return toBasicUnit ([l, r], o);}


orderArray
  = first:(s:SIGNED_ID COMMA {return s;})* last:SIGNED_ID {return first.concat(last);}

orderFilter
  = _ o:"sort" OPAR list: orderArray CPAR
    {return toBasicUnit (list, o);}

limitFilter
  = _ o:"limit" OPAR size: INT CPAR
    {return toLimitUnit (size);}
  / _ o:"limit" OPAR from: INT COMMA size: INT CPAR
    {return toLimitUnit (from, size);}

orderAndLimitFilter
  = order: orderFilter COMMA limit: limitFilter
    {return [order, limit];}


valueArray
  = stringArgArray
  / numberArray

VALUE
  = STRING
  / NUMBER


/**
* Data types
*/

ASTERISK = "*"
COMMA = _ "," _
DOUBLE_QUOTE = '"'
SINGLE_QUOTE = "'"
DECIMAL_POINT = "."

OPAR = _ "(" _
CPAR = _ ")" _

ID
  = first: [a-zA-Z_] rest: [a-zA-Z_0-9.]*
    {return headJoinsTail (first, rest);}


CHAR = [ a-zA-Z.0-9_:!()@#$%^&=+{}|\-,.<>?/~`]

STRING
  = _ DOUBLE_QUOTE first: CHAR rest: CHAR* DOUBLE_QUOTE _
    {return headJoinsTail (first, rest);}
  / _ SINGLE_QUOTE first: CHAR rest: CHAR* SINGLE_QUOTE _
    {return headJoinsTail (first, rest);}

SIGN = "+" / "-"

SIGNED_ID
  = _ sign: SIGN? id: ID
    {return toSortUnit (id, sign);}

number_frac
  = DECIMAL_POINT digits: [0-9]*
    {return headJoinsTail (".", digits);}

NUMBER
  = sign: SIGN? digits: [0-9]+ frac: number_frac?
    {var num = parseFloat (digits.join("") + frac);
return (sign === "-") ? (num * -1) : num;
}


INT
  = digits: [0-9]+
    { var num = parseInt (digits.join(""), 10); return num;}

// Whitespaces
_ = [ \r\n\t]*

