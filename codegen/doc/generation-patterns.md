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
* not shown above, but related1C is required and single valued
* not shown above, but optionalRelated2 is optional and multivalued, so it is a collection.

In Java:
```
    this.removeFromOptionalRelated2(given);
    given.setRelated1C(null);
```
