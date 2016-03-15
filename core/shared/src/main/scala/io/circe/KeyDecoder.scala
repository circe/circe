package io.circe

import cats.MonadError

/**
 * A type class that provides a conversion from a string used as a JSON key to a
 * value of type `A`.
 */
abstract class KeyDecoder[A] extends Serializable { self =>
  /**
   * Convert a value to String.
   */
  def apply(key: String): Option[A]

  /**
   * Construct an instance for type `B` from an instance for type `A`.
   */
  final def map[B](f: A => B): KeyDecoder[B] = new KeyDecoder[B] {
    final def apply(key: String): Option[B] = self(key).map(f)
  }

  /**
   * Construct an instance for type `B` from an instance for type `A` given a
   * monadic function.
   */
  final def flatMap[B](f: A => KeyDecoder[B]): KeyDecoder[B] = new KeyDecoder[B] {
    final def apply(key: String): Option[B] = self(key).flatMap(a => f(a)(key))
  }
}

final object KeyDecoder {
  def apply[A](implicit A: KeyDecoder[A]): KeyDecoder[A] = A

  def instance[A](f: String => Option[A]): KeyDecoder[A] = new KeyDecoder[A] {
    final def apply(key: String): Option[A] = f(key)
  }

  private[this] def numberInstance[A](f: String => A): KeyDecoder[A] = new KeyDecoder[A] {
    final def apply(key: String): Option[A] = try Some(f(key)) catch {
      case _: NumberFormatException => None
    }
  }

  implicit val decodeKeyString: KeyDecoder[String] = new KeyDecoder[String] {
    final def apply(key: String): Option[String] = Some(key)
  }

  implicit val decodeKeySymbol: KeyDecoder[Symbol] = new KeyDecoder[Symbol] {
    final def apply(key: String): Option[Symbol] = Some(Symbol(key))
  }

  implicit val decodeKeyByte: KeyDecoder[Byte] = numberInstance(_.toByte)
  implicit val decodeKeyShort: KeyDecoder[Short] = numberInstance(_.toShort)
  implicit val decodeKeyInt: KeyDecoder[Int] = numberInstance(_.toInt)
  implicit val decodeKeyLong: KeyDecoder[Long] = numberInstance(_.toLong)

  implicit val monadErrorKeyDecode: MonadError[KeyDecoder, Unit] = new MonadError[KeyDecoder, Unit] {
    final def pure[A](a: A): KeyDecoder[A] = new KeyDecoder[A] {
      final def apply(key: String): Option[A] = Some(a)
    }

    override final def map[A, B](fa: KeyDecoder[A])(f: A => B): KeyDecoder[B] = fa.map(f)

    final def flatMap[A, B](fa: KeyDecoder[A])(f: A => KeyDecoder[B]): KeyDecoder[B] = fa.flatMap(f)

    final def raiseError[A](e: Unit): KeyDecoder[A] = new KeyDecoder[A] {
      final def apply(key: String): Option[A] = None
    }

    final def handleErrorWith[A](fa: KeyDecoder[A])(f: Unit => KeyDecoder[A]): KeyDecoder[A] = new KeyDecoder[A] {
      final def apply(key: String): Option[A] = f(())(key)
    }
  }
}
