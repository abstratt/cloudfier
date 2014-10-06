package com.abstratt.mdd.target.tests.jpa;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.AssertHelper;

public class JPABasicTests extends AbstractRepositoryBuildingTests {

    public static Test suite() {
        return new TestSuite(JPABasicTests.class);
    }

    private ITopLevelMapper jpaMapper;

    public JPABasicTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ITargetPlatform platform = TargetCore.getBuiltInPlatform("jpa");
        Assert.assertNotNull(platform);
        jpaMapper = platform.getMapper(null);
        Assert.assertNotNull(jpaMapper);
    }

    public void testEmptyClass() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Account\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import javax.persistence.*; @Entity public class Account {}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testManyToMany() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Customer\n";
        source += "    readonly attribute accounts : Account[*];\n";
        source += "  end;\n";
        source += "  class Account\n";
        source += "    attribute owners : Customer[*];\n";
        source += "  end;\n";
        source += "  association role Customer.accounts; role Account.owners; end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Customer", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import java.util.Set; import javax.persistence.*; @Entity public class Customer { private Set<Account> accounts; @ManyToMany public Set<Account> getAccounts() { return this.accounts; }}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testManyToOne() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Customer\n";
        source += "    attribute accounts : Account[*];\n";
        source += "  end;\n";
        source += "  class Account\n";
        source += "    readonly attribute owner : Customer;\n";
        source += "  end;\n";
        source += "  association role Customer.accounts; role Account.owner; end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import javax.persistence.*; @Entity public class Account { private Customer owner; @ManyToOne(optional=false) public Customer getOwner() { return this.owner; }}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testManyToOneNoOpposite() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Customer\n";
        source += "  end;\n";
        source += "  class Account\n";
        source += "    readonly reference owner : Customer;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import javax.persistence.*; @Entity public class Account { private Customer owner; @ManyToOne(optional=false) public Customer getOwner() { return this.owner; }}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testOneToMany() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Customer\n";
        source += "    readonly attribute accounts : Account[*];\n";
        source += "  end;\n";
        source += "  class Account\n";
        source += "    attribute owner : Customer;\n";
        source += "  end;\n";
        source += "  association role Customer.accounts; role Account.owner; end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Customer", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import java.util.Set; import javax.persistence.*; @Entity public class Customer { private Set<Account> accounts; @OneToMany public Set<Account> getAccounts() { return this.accounts; }}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testOneToOne() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  class Customer\n";
        source += "    attribute account : Account;\n";
        source += "  end;\n";
        source += "  class Account\n";
        source += "    readonly attribute owner : Customer;\n";
        source += "  end;\n";
        source += "  association role Customer.account; role Account.owner; end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import javax.persistence.*; @Entity public class Account { private Customer owner; @OneToOne(optional=false) public Customer getOwner() { return this.owner; }}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testPublicAttribute() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class Account\n";
        source += "    attribute name : String;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = jpaMapper.map(clazz).toString();
        String expected = "package simple; import javax.persistence.*; @Entity public class Account { private String name; public String getName(){return this.name;} public void setName(String name){this.name=name;}}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

}
