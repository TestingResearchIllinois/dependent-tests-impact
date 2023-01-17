package edu.washington.cs.dt.impact.util;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.*;


public class Method {
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private long time;
    @JacksonXmlProperty(isAttribute = true)
    private boolean throwException=false;

    public ArrayList<Method> method;

    public Method(String id,String name,long time,boolean throwException) {
        this.id = id;
        this.name=name;
        this.time=time;
        this.throwException=throwException;
        this.method = new ArrayList<Method>();
    }

    public String addChild(String parentname,String id,String name,long time,boolean throwException) {
        ListIterator<Method> e
                = method.listIterator();
        int i=0;
        while ((e.hasNext())){
            Method temp=e.next();
            if(Objects.equals(temp.getName(), parentname))
            {
                ListIterator<Method> internal
                        = temp.getMethod().listIterator();
                String lastChildId=id;
                while (internal.hasNext())
                {
                    Method tempChild = internal.next();
                    try {
                        lastChildId = tempChild.getId();
                    }catch (NoSuchElementException err)
                    {
                        continue;
                    }
                }
                String[] arrOfStr = lastChildId.split("\\.");
                int lastInternal=Integer.parseInt(arrOfStr[arrOfStr.length-1])+1;
                String lastPartInternal=Integer.toString(lastInternal);
                String internalID=temp.getId()+"."+lastPartInternal;
                i=1;
                try{
                    temp.getMethod().add(new Method(internalID,name,time,throwException));
                    long newtime= temp.getTime()+time;
                    temp.setTime(newtime);
                    return internalID;
                }
                catch (NoSuchElementException err)
                {
                    return internalID;
                }

            }
        }

        if(i!=1)
        {
            String[] arrOfStr = id.split("\\.");
            int last=Integer.parseInt(arrOfStr[0])+1;
            String lastPart=Integer.toString(last);
            String idChild=this.getId()+"."+lastPart;
            Method newParent= new Method(idChild,parentname,time,throwException);
            this.method.add(newParent);
            newParent.getMethod().add(new Method(idChild+"."+"1",name,time,throwException));
            return idChild+"."+"1";
        }
        return id;
    }


    public void addTime(long time)
    {
        this.time+=time;
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

    public List<Method> getMethod() {
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

