package io.github.alktrip.cfdi4j.pagos20;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Copies of model records with one component changed, to build invalid cases the builder refuses. */
final class Copias {

    private Copias() {}

    /** Returns a copy of {@code record} with the component {@code name} set to {@code value}. */
    static <T extends Record> T con(T record, String name, Object value) {
        RecordComponent[] components = record.getClass().getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        boolean found = false;
        try {
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                if (components[i].getName().equals(name)) {
                    args[i] = value;
                    found = true;
                } else {
                    args[i] = components[i].getAccessor().invoke(record);
                }
            }
            if (!found) {
                throw new IllegalArgumentException(record.getClass().getSimpleName() + " has no component " + name);
            }
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = (Constructor<T>) record.getClass().getDeclaredConstructor(types);
            return ctor.newInstance(args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Applies {@code f} to the first Pago. */
    static Pagos primerPago(Pagos pagos, UnaryOperator<Pagos.Pago> f) {
        List<Pagos.Pago> lista = new ArrayList<>(pagos.pago());
        lista.set(0, f.apply(lista.get(0)));
        return new Pagos(pagos.totales(), lista);
    }

    /** Applies {@code f} to the first DoctoRelacionado of the first Pago. */
    static Pagos primerDocto(Pagos pagos, UnaryOperator<Pagos.DoctoRelacionado> f) {
        return primerPago(pagos, p -> {
            List<Pagos.DoctoRelacionado> docs = new ArrayList<>(p.doctoRelacionado());
            docs.set(0, f.apply(docs.get(0)));
            return con(p, "doctoRelacionado", docs);
        });
    }
}
