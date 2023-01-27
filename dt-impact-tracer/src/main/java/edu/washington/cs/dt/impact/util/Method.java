package edu.washington.cs.dt.impact.util;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.*;


public class Method {
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private boolean testType=true;
    @JacksonXmlProperty(isAttribute = true)
    private long time;
    @JacksonXmlProperty(isAttribute = true)
    private boolean throwException=false;

    public ArrayList<Method> method;

    public Method(String id,String name,long time,boolean testType,boolean throwException) {
        this.id = id;
        this.name=name;
        this.testType=testType;
        this.time=time;
        this.throwException=throwException;
        this.method = new ArrayList<Method>();
    }

    public Method findParent(String parentname,ArrayList<Method> meth,long time,Method res,boolean throwException)
    {
        ListIterator<Method> e
                = meth.listIterator(meth.size());
        while ((e.hasPrevious())) {
            Method temp = e.previous();
            if(Objects.equals(temp.getName(), parentname))
            {
                res=temp;
                break;
            }
            long newtime= temp.getTime()+time;
            temp.setTime(newtime);
            return findParent(parentname,temp.getMethod(),time,res,throwException);

        }
        return res;

    }
    public String addChild(String parentname,String id,String name,long time,boolean throwException) {
        int i=0,expFlag=0;
        if(name.contains("Exception:"))
        {
            throwException=true;
        }
        Method temp=findParent(parentname,this.method,time,null,throwException);
        if(temp!=null)
        {
            temp.setThrowException(throwException);
            ListIterator<Method> internal
                    = temp.getMethod().listIterator();
            String lastChildId=temp.getId();
            int hasChild=0;
            while (internal.hasNext())
            {
                Method tempChild = internal.next();
                try {
                    hasChild=1;
                    lastChildId = tempChild.getId();
                }catch (NoSuchElementException err)
                {
                    continue;
                }
            }
            String internalID=id;
            if(hasChild==0)
            {
                internalID=lastChildId+".1";
            }
            else {
                String[] arrOfStr = lastChildId.split("\\.");
                int lastInternal=Integer.parseInt(arrOfStr[arrOfStr.length-1])+1;
                String lastPartInternal=Integer.toString(lastInternal);
                internalID=temp.getId()+"."+lastPartInternal;
            }

            i=1;
            try{
                temp.getMethod().add(new Method(internalID,name,time,false,throwException));
                long newtime= temp.getTime()+time;
                temp.setTime(newtime);
                //this.addAllTime(time,temp.getMethod(),parentname);
                return internalID;
            }
            catch (NoSuchElementException err)
            {
                return internalID;
            }
        }else
        {
            String[] arrOfStr = id.split("\\.");
            int last=Integer.parseInt(arrOfStr[0])+1;
            String lastPart=Integer.toString(last);
            String idChild=this.getId()+"."+lastPart;
            Method newParent= new Method(idChild,parentname,time,true,throwException);
            this.method.add(newParent);
            newParent.getMethod().add(new Method(idChild+"."+"1",name,time,false,throwException));
            return idChild+"."+"1";
        }
    }


    public void addTime()
    {
        ArrayList<Method> meth=this.method;
        ListIterator<Method> e
                = meth.listIterator();
        int i=0;
        while ((e.hasNext())) {
            Method newTemp= e.next();
            System.out.println("----name"+newTemp.getName());

        }
        //System.out.println("-----depth----"+i);
    }

    public void addAllTime(long time,ArrayList<Method> meth,String parentname)
    {
        System.out.println("-----parent----"+parentname);
        ListIterator<Method> e
                = meth.listIterator(meth.size());
        int i=0;
        while ((e.hasPrevious())) {
            Method newTemp= e.previous();
            if(i==1)
            {
                System.out.println("-----addeed----"+newTemp.getName());
                long newtime= newTemp.getTime()+time;
                newTemp.setTime(newtime);
            }
            if(Objects.equals(newTemp.getName(), parentname))
            {
                System.out.println("-----not----"+newTemp.getName());
                i=1;
            }

        }
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public ArrayList<Method> getMethod() {
        return method;
    }

    public void setMethod(ArrayList<Method> method) {
        this.method = method;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
}

