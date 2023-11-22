package net.minecraft.core;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;

public class NonNullList<E> extends AbstractList<E>
{
    private final List<E> list;
    @Nullable
    private final E defaultValue;

    public static <E> NonNullList<E> create()
    {
        return new NonNullList<>(Lists.newArrayList(), (E)null);
    }

    public static <E> NonNullList<E> createWithCapacity(int p_182648_)
    {
        return new NonNullList<>(Lists.newArrayListWithCapacity(p_182648_), (E)null);
    }

    public static <E> NonNullList<E> withSize(int pSize, E pDefaultValue)
    {
        Validate.notNull(pDefaultValue);
        Object[] aobject = new Object[pSize];
        Arrays.fill(aobject, pDefaultValue);
        return new NonNullList<>(Arrays.asList((E[])aobject), pDefaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> a(E p_122784_, E... p_122785_)
    {
        return new NonNullList<>(Arrays.asList(p_122785_), p_122784_);
    }

    protected NonNullList(List<E> pList, @Nullable E pDefaultValue)
    {
        this.list = pList;
        this.defaultValue = pDefaultValue;
    }

    @Nonnull
    public E get(int pIndex)
    {
        return this.list.get(pIndex);
    }

    public E set(int pIndex, E pValue)
    {
        Validate.notNull(pValue);
        return this.list.set(pIndex, pValue);
    }

    public void add(int pIndex, E pValue)
    {
        Validate.notNull(pValue);
        this.list.add(pIndex, pValue);
    }

    public E remove(int pIndex)
    {
        return this.list.remove(pIndex);
    }

    public int size()
    {
        return this.list.size();
    }

    public void clear()
    {
        if (this.defaultValue == null)
        {
            super.clear();
        }
        else
        {
            for (int i = 0; i < this.size(); ++i)
            {
                this.set(i, this.defaultValue);
            }
        }
    }
}
