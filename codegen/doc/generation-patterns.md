## Detaching the current object from another

In TextUML:

```
operation op1(given : MyClass2);
begin
    unlink ManyMyClass2OneMyClass1(related1C := self, optionalRelated2 := given);
end;
```

Which means self and 'given', which are (supposed) related via ManyMyClass2OneMyClass1, should become detached.

Things to think about:
- not shown above, but related1C is required and single valued
- not shown above, but optionalRelated2 is optional and multivalued, so it is a collection.

In Java:
```
    this.removeFromOptionalRelated2(given);
    given.setRelated1C(null);
```

## Null-safety

Given a model fragment like this:
```
operation op1(val1 : MyClass1[0,1]);
begin
    val1.anAttribute := 1;
end;
```

When generating code, we need to take into account this is a tentative assignment - since val1 could be null, and in that case, we should only perform the statement if val1 has a value.

Suggested Java code:

```
if (val1 != null) {
    val1.setAnAttribute(1L);
}
```

But what if we had a model like this:

```
operation op1(val1 : MyClass1);
begin
    self.someAttribute := val1.otherAttribute;
end;
```
where val1 and self.someAttribute are required, but val1.otherAttribute is optional,  
```
if (val1 != null) {
    val1.setAnAttribute(1L);
}
```

