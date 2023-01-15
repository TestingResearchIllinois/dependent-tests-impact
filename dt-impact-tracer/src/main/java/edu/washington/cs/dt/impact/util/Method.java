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

    public void addChild(String parentname,String id,String name,long time,boolean throwException) {
        ListIterator<Method> e
                = method.listIterator();;
        int i=0;
        String idChild=this.getId()+"."+id;
        while ((e.hasNext())){
            Method temp=e.next();
            if(Objects.equals(temp.getName(), parentname))
            {
                String internalID=temp.getId()+"."+id;
                i=1;
                try{
                    temp.getMethod().add(new Method(internalID,name,time,throwException));
                    long newtime= temp.getTime()+time;
                    temp.setTime(newtime);
                    break;
                }
                catch (NoSuchElementException err)
                {
                    break;
                }

            }
        }
        if(i!=1)
        {

            Method newParent= new Method(idChild,parentname,time,throwException);
            this.method.add(newParent);
            newParent.getMethod().add(new Method(idChild+"."+id,name,time,throwException));
            //this.method.id(idChild);
            //this.method.add(newParent);
            //this.addChild(parentname,idChild,name,time,throwException);


        }
    }


    public void addTime(long time)
    {
        this.time+=time;
        /*this.time=this.getTime()+time;
        ListIterator<Method> e
                = method.listIterator();;
        while ((e.hasNext())){
            Method temp=e.next();

            if(Objects.equals(temp.getName(), parentname))
            {
                temp.time=time+ temp.getTime();
                break;
            }
        }*/
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

