package l2p.commons.lang.reference;

/**
 * Интерфейс хранителя ссылки.
 *
 * @author G1ta0
 *
 * @param <T>
 */
public interface HardReference<T> {

    /**
     * Получить объект, который удерживается *
     */
    T get();

    /**
     * Очистить сылку на удерживаемый объект *
     */
    void clear();
}
